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

    public void toDOT(String f_out, String graphName, double[] x, double[] q, double threshold) {
        try (PrintWriter out = new PrintWriter(f_out + graphName + ".dot", StandardCharsets.UTF_8)) {
            out.println("digraph " + graphName + " {");
            namingMap.forEach((k, v) -> {
                String color = (q[k] > threshold) ? "blue" : "black";
                //out.println("N_" + k + " [shape=circle, color=" + color + ", label=\"" + v + " : " + String.format("%.4f", q[k]) + "\"];");
                out.println("N_" + k + " [shape = box, color = " + color + ", label = \"" + v + "\"];");
            });
            for (int i = 0; i < edgesList.size(); i++) {
                Pair<Integer, Integer> p = edgesList.get(i);
                String color = (Math.abs(x[i] - 1.0) < 1e-5) ? "red" : "black";
                out.println("N_" + p.first + " -> " + "N_" + p.second + " [ color = " + color + " ];");
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
