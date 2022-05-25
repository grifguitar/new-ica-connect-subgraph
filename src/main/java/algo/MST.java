package algo;

import graph.Graph;
import utils.Pair;

import java.util.*;

public class MST {
    public static void solve(
            Graph graph,
            double[] x,
            double[] q,
            double[] r,
            double STEP
    ) {
        List<Pair<Double, Integer>> undirected_edges = new ArrayList<>();

        if (x.length % 2 != 0) {
            throw new RuntimeException("unexpected edges count");
        }

        for (int num = 0; num < x.length; num += 2) {
            int back_num = Graph.companionEdge(num);
            if (back_num != num + 1) {
                throw new RuntimeException("unexpected back_num");
            }

            Graph.checkEdges(graph, num, back_num);

            undirected_edges.add(
                    new Pair<>(
                            Math.max(x[num], x[back_num]),
                            num
                    )
            );
        }

        undirected_edges.sort(Comparator.comparing(p -> p.first));
        Collections.reverse(undirected_edges);

        DSU dsu = new DSU(graph.getNodesCount());

        Set<Integer> ans_edges = new HashSet<>();

        for (Pair<Double, Integer> elem : undirected_edges) {
            int num = elem.second;
            Pair<Integer, Integer> edge = graph.getEdges().get(num);
            if (dsu.unionSets(edge.first, edge.second)) {
                ans_edges.add(num);
            }
        }

        List<List<Pair<Integer, Long>>> g = new ArrayList<>();
        for (int i = 0; i < graph.getNodesCount(); i++) {
            g.add(new ArrayList<>());
        }

        for (Integer num : ans_edges) {
            Pair<Integer, Integer> edge = graph.getEdges().get(num);
            g.get(edge.first).add(new Pair<>(edge.second, (long) num));
            g.get(edge.second).add(new Pair<>(edge.first, (long) Graph.companionEdge(num)));
        }

        main_solve(g, x, q, r, STEP);
    }

    private static void main_solve(List<List<Pair<Integer, Long>>> g, double[] x, double[] q, double[] r, double STEP) {
        double q_max = -1;
        int root = -1;
        for (int i = 0; i < q.length; i++) {
            if (q[i] > q_max) {
                q_max = q[i];
                root = i;
            }
        }

//        double r_max = -1;
//        int root = -1;
//        for (int i = 0; i < r.length; i++) {
//            if (r[i] > r_max) {
//                r_max = r[i];
//                root = i;
//            }
//        }

        check_mst(g, q, root);

        Arrays.fill(r, 0);
        r[root] = 1;

        Arrays.fill(x, 0);

        //check_mst_ordered(g, q, x, root, STEP, false);
        //check_mst_ordered(g, q, x, root, STEP, true);
        check_mst_ordered(g, q, x, root, STEP, false);
    }

    private static void check_mst(List<List<Pair<Integer, Long>>> g, double[] q, int root) {
        int[] vis = new int[g.size()];

        check_dfs_mst(g, vis, q, root, -1);

        for (int i = 0; i < vis.length; i++) {
            if (vis[i] != 2) throw new RuntimeException("non-correct MST in vertex: " + i);
        }
    }

    private static void check_dfs_mst(List<List<Pair<Integer, Long>>> g, int[] vis, double[] q, int v, int parent) {
        vis[v] = 1;
        for (Pair<Integer, Long> pair : g.get(v)) {
            int to = pair.first;
            if (to != parent) {
                if (vis[to] == 0) {
                    check_dfs_mst(g, vis, q, to, v);
                } else {
                    throw new RuntimeException("unexpected!");
                }
            }
        }
        vis[v] = 2;
        tuning(g, q, v, parent);
    }

    private static void tuning(List<List<Pair<Integer, Long>>> g, double[] q, int currVertex, int parVertex) {
        List<Integer> changed = new ArrayList<>();

        changed.add(currVertex);
        double total_sum = q[currVertex];
        int total_count = 1;
        double average = total_sum / (double) total_count;

        TreeMap<Double, List<Pair<Integer, Integer>>> mp = new TreeMap<>();

        Pair<Integer, Integer> elem2 = new Pair<>(currVertex, parVertex);
        for (Pair<Integer, Long> pair : g.get(elem2.first)) {
            int to = pair.first;
            if (to != elem2.second) {
                if (!mp.containsKey(q[to])) {
                    mp.put(q[to], new ArrayList<>());
                }
                mp.get(q[to]).add(new Pair<>(to, elem2.first));
            }
        }

        while (!mp.isEmpty()) {
            double key = mp.lastKey();
            if (average >= key) {
                break;
            }

            List<Pair<Integer, Integer>> list_by_key = mp.remove(key);
            assert list_by_key.size() != 0;
            Pair<Integer, Integer> elem = list_by_key.remove(list_by_key.size() - 1);
            if (!list_by_key.isEmpty()) {
                mp.put(key, list_by_key);
            }

            for (Pair<Integer, Long> pair : g.get(elem.first)) {
                int to = pair.first;
                if (to != elem.second) {
                    if (!mp.containsKey(q[to])) {
                        mp.put(q[to], new ArrayList<>());
                    }
                    mp.get(q[to]).add(new Pair<>(to, elem.first));
                }
            }

            changed.add(elem.first);
            total_sum += key;
            total_count += 1;
            average = total_sum / (double) total_count;
        }

        for (int vertex : changed) {
            q[vertex] = average;
        }
    }

    private static void check_mst_ordered(List<List<Pair<Integer, Long>>> g, double[] q, double[] x, int root, double STEP, boolean MODIFY) {
        int[] vis = new int[g.size()];

        check_dfs_mst_ordered(g, vis, q, x, root, -1, STEP, MODIFY);

        for (int i = 0; i < vis.length; i++) {
            if (vis[i] != 2) throw new RuntimeException("non-correct MST in vertex: " + i);
        }
    }

    private static void check_dfs_mst_ordered(List<List<Pair<Integer, Long>>> g, int[] vis, double[] q, double[] x, int v, int parent, double STEP, boolean MODIFY) {
        vis[v] = 1;
        for (Pair<Integer, Long> pair : g.get(v)) {
            int to = pair.first;
            if (to != parent) {
                if (vis[to] == 0) {
                    x[pair.second.intValue()] = 1;
                    if (q[v] < q[to]) {
                        throw new RuntimeException("something wrong");
                    }
                    check_dfs_mst_ordered(g, vis, q, x, to, v, STEP, MODIFY);
                    if (MODIFY) {
                        if (q[v] <= q[to]) {
                            q[v] = q[to] + STEP;
                        }
                    }
                } else {
                    throw new RuntimeException("unexpected!");
                }
            }
        }
        vis[v] = 2;
    }

}
