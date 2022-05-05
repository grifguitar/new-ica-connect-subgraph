package algo;

public class DSU {
    private final int[] parent;
    private final int[] rank;

    public DSU(int n) {
        this.parent = new int[n];
        this.rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }

    public int findSet(int v) {
        if (v == parent[v]) {
            return v;
        }
        parent[v] = findSet(parent[v]);
        return parent[v];
    }

    public boolean unionSets(int a, int b) {
        a = findSet(a);
        b = findSet(b);
        if (a != b) {
            if (rank[a] < rank[b]) {
                parent[a] = b;
                if (rank[a] == rank[b]) {
                    rank[b]++;
                }
            } else {
                parent[b] = a;
                if (rank[a] == rank[b]) {
                    rank[a]++;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
