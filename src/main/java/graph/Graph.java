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

    public void saveAsDOT(String folder, String graphName, Double[] x, Double[] q, Pair<Double, Boolean[]> modules, int module) {
        try (PrintWriter out = new PrintWriter(
                folder + graphName + "_module" + module + ".dot", StandardCharsets.UTF_8
        )) {
            out.println("digraph " + graphName + " {");
            namingMap.forEach((k, v) -> {

                String color;
                String shape;

                boolean isPredict1 = (q[k] > modules.first);
                Boolean isTrue1 = (modules.second[k]);

                if (isTrue1) {
                    shape = "ellipse";
                } else {
                    shape = "box";
                }

                switch (shape) {
                    case "ellipse":
                        if (isPredict1) {
                            color = "green";
                        } else {
                            color = "red";
                        }
                        break;
                    case "box":
                        if (isPredict1) {
                            color = "orange";
                        } else {
                            color = "lightgray";
                        }
                        break;
                    default:
                        throw new RuntimeException("unexpected shape");
                }

                out.println("N_" + k + " [shape = " + shape + ", style = filled, fillcolor = " + color + ", label = \""
                        + v + "\\n" + String.format("%.3f", q[k]) + "\"];");
            });
            for (int i = 0; i < edgesList.size(); i++) {
                Pair<Integer, Integer> p = edgesList.get(i);
                if ((Math.abs(x[i] - 1.0) < 1e-5)) {
                    out.println("N_" + p.first + " -> " + "N_" + p.second + " [ color = " + "blue" + " ];");
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
