package algo;

import graph.Graph;
import utils.Pair;

import java.util.*;

public class MST {
    public static void solve(
            Graph graph,
            double[] x,
            double[] q,
            double[] r
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
                            Math.max(x[num], x[num + 1]),
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

        List<List<Integer>> g = new ArrayList<>();
        List<List<Pair<Integer, Long>>> g_ext = new ArrayList<>();
        for (int i = 0; i < graph.getNodesCount(); i++) {
            g.add(new ArrayList<>());
            g_ext.add(new ArrayList<>());
        }

        for (Integer num : ans_edges) {
            Pair<Integer, Integer> edge = graph.getEdges().get(num);
            g.get(edge.first).add(edge.second);
            g.get(edge.second).add(edge.first);
            g_ext.get(edge.first).add(new Pair<>(edge.second, (long) num));
            g_ext.get(edge.second).add(new Pair<>(edge.first, (long) Graph.companionEdge(num)));
        }

        check_mst(g);

        double q_max = -1;
        int root = -1;
        for (int i = 0; i < q.length; i++) {
            if (q[i] > q_max) {
                q_max = q[i];
                root = i;
            }
        }

        heuristic_dfs(g, root, -1, q);

        Arrays.fill(x, 0);
        Arrays.fill(r, 0);

        r[root] = 1;

        check_ordered(g_ext, q, root, x);
    }

    private static void heuristic_dfs(List<List<Integer>> g, int v, int parent, double[] q) {
        for (int to : g.get(v)) {
            if (to != parent) {
                heuristic_dfs(g, to, v, q);
                tuning(g, v, to, q);
            }
        }
    }

    private static void tuning(List<List<Integer>> g, int new_root, int root, double[] q) {
        List<Integer> changed = new ArrayList<>();

        changed.add(new_root);
        double total_sum = q[new_root];
        int total_count = 1;
        double average = total_sum / (double) total_count;

        TreeSet<MyData> st = new TreeSet<>();
        st.add(new MyData(q[root], root, new_root));

        while (!st.isEmpty()) {
            MyData elem = st.pollLast();
            assert elem != null;
            if (average < elem.key) {
                for (int w : g.get(elem.number)) {
                    if (w != elem.parent) {
                        st.add(new MyData(q[w], w, elem.number));
                    }
                }
                changed.add(elem.number);
                total_sum += elem.key;
                total_count += 1;
                average = total_sum / (double) total_count;
            } else {
                break;
            }
        }

        for (int vertex : changed) {
            q[vertex] = average;
        }
    }

    private static void check_ordered(List<List<Pair<Integer, Long>>> g, double[] q, int root, double[] x) {
        int[] vis = new int[g.size()];

        check_dfs_ordered(g, vis, root, -1, q, x);

        for (int i = 0; i < vis.length; i++) {
            if (vis[i] != 2) throw new RuntimeException("non-correct MST in vertex: " + i);
        }
    }

    private static void check_dfs_ordered(
            List<List<Pair<Integer, Long>>> g, int[] vis, int v, int parent, double[] q, double[] x
    ) {
        vis[v] = 1;
        for (Pair<Integer, Long> pair : g.get(v)) {
            int to = pair.first;
            int num = pair.second.intValue();
            if (to != parent) {
                if (vis[to] == 0) {
                    if (q[to] > q[v]) {
                        throw new RuntimeException("unordered mst!");
                    }
                    x[num] = 1;
                    check_dfs_ordered(g, vis, to, v, q, x);
                } else if (vis[to] == 1) {
                    throw new RuntimeException("find cycle!");
                } else if (vis[to] == 2) {
                    throw new RuntimeException("unexpected edge!");
                }
            }
        }
        vis[v] = 2;
    }

    private static void check_mst(List<List<Integer>> g) {
        int[] vis = new int[g.size()];

        check_dfs_mst(g, vis, 0, -1);

        for (int i = 0; i < vis.length; i++) {
            if (vis[i] != 2) throw new RuntimeException("non-correct MST in vertex: " + i);
        }
    }

    private static void check_dfs_mst(List<List<Integer>> g, int[] vis, int v, int parent) {
        vis[v] = 1;
        for (int to : g.get(v)) {
            if (to != parent) {
                if (vis[to] == 0) {
                    check_dfs_mst(g, vis, to, v);
                } else if (vis[to] == 1) {
                    throw new RuntimeException("find cycle!");
                } else if (vis[to] == 2) {
                    throw new RuntimeException("unexpected edge!");
                }
            }
        }
        vis[v] = 2;
    }

    private static class MyData implements Comparable<MyData> {
        public double key;
        public int number;
        public int parent;

        public MyData(double key, int number, int parent) {
            this.key = key;
            this.number = number;
            this.parent = parent;
        }

        @Override
        public int compareTo(MyData other) {
            return Double.compare(this.key, other.key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyData myData = (MyData) o;

            if (Double.compare(myData.key, key) != 0) return false;
            if (number != myData.number) return false;
            return parent == myData.parent;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(key);
            result = (int) (temp ^ (temp >>> 32));
            result = 31 * result + number;
            result = 31 * result + parent;
            return result;
        }
    }
}
