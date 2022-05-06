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

    public void saveAsDOT(String folder, String graphName, Double[] x, Double[] q, List<Pair<Double, Boolean[]>> modules) {
        try (PrintWriter out = new PrintWriter(folder + graphName + ".dot", StandardCharsets.UTF_8)) {
            out.println("digraph " + graphName + " {");
            namingMap.forEach((k, v) -> {
                String color = (q[k] > modules.get(2).first) ? "red" : "yellow";
                String shape = (modules.get(2).second[k]) ? "ellipse" : "box";
                if (color.equals("yellow") && shape.equals("ellipse")) {
                    color = "green";
                }
                out.println("N_" + k + " [shape = " + shape + ", style = filled, fillcolor = " + color + ", label = \""
                        + v + "\\n" + String.format("%.4f", q[k]) + "\"];");
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
