package graph;

import utils.Pair;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Graph(
        Map<Integer, String> namingMap,
        List<List<Pair<Integer, Long>>> graph,
        List<Pair<Integer, Integer>> edgesList
) {

    public List<Pair<Integer, Integer>> getEdges() {
        return edgesList;
    }

    public List<Pair<Integer, Long>> edgesOf(int v) {
        return graph.get(v);
    }

    public int getNodesCount() {
        return graph.size();
    }

    public void saveAsDOT(String folder, String graphName, Double[] x, Double[] q, Pair<Double, Boolean[]> modules, int module, boolean NO_OPT) {
        try (PrintWriter out = new PrintWriter(
                folder + graphName + "_module" + module + ".dot", StandardCharsets.UTF_8
        )) {
            final double[] q_cff = {0, 1};
            final int[] cnt = {0};
            namingMap.forEach((k, v) -> {
                if (q[k] > q_cff[0]) {
                    q_cff[0] = q[k];
                }
                if (q[k] > modules.first) {
                    q_cff[1] += q[k];
                    cnt[0]++;
                }
            });
            q_cff[1] = q_cff[1] / cnt[0];
            double scale = 100.0 / (q_cff[1] - modules.first);
            out.println("digraph " + graphName + " {");
            namingMap.forEach((k, v) -> {

                String color;
                String shape;

                boolean isPredict1 = (q[k] > modules.first);
                Boolean isTrue1 = (modules.second[k]);

                long intensive = 75 + Math.round((q[k] - modules.first) * scale);
                if (intensive >= 255) intensive = 255;
                if (intensive <= 75) intensive = 75;
                String hexIntensive = Long.toHexString(intensive);

                if (isTrue1) {
                    shape = "ellipse";
                } else {
                    shape = "box";
                }

                if (isPredict1) {
                    color = "#96" + hexIntensive + "00";
                } else {
                    color = "#ff0000";
                }

                if (NO_OPT || isPredict1) {
                    out.println("N_" + k + " [shape = " + shape + ", style = filled, fillcolor = \"" + color + "\", label = \""
                            + v + "\\n" + String.format("%.3f", q[k]) + "\"];");
                }
            });
            for (int i = 0; i < edgesList.size(); i++) {
                Pair<Integer, Integer> p = edgesList.get(i);
                if ((Math.abs(x[i] - 1.0) < 1e-5)) {
                    if ((q[p.first] > modules.first) && (q[p.second] > modules.first)) {
                        out.println("N_" + p.first + " -> " + "N_" + p.second + " [ color = " + "blue" + " ];");
                    } else if (NO_OPT) {
                        out.println("N_" + p.first + " -> " + "N_" + p.second + " [ color = " + "yellow" + " ];");
                    }
                }
            }
            out.println("}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int companionEdge(int num) {
        if (num % 2 == 0) {
            return num + 1;
        } else {
            return num - 1;
        }
    }

    public static void checkEdges(Graph graph, int num, int back_num) {
        Pair<Integer, Integer> edge = graph.getEdges().get(num);
        Pair<Integer, Integer> back_edge = graph.getEdges().get(back_num);

        if (!Objects.equals(edge.first, back_edge.second) ||
                !Objects.equals(edge.second, back_edge.first)) {
            throw new RuntimeException("unexpected edge or back_edge");
        }
    }

    public static void checkDest(Graph graph, int back_num, int to) {
        Pair<Integer, Integer> back_edge = graph.getEdges().get(back_num);

        if (back_edge.second != to) {
            throw new RuntimeException("unexpected back_edge destination");
        }
    }

}
