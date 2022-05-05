package graph;

import utils.Pair;

import java.util.List;
import java.util.Objects;

public record Graph(
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
