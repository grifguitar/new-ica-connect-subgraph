package solver;

import algo.MST;
import drawing.DrawUtils;
import graph.Graph;
import ilog.concert.*;
import ilog.cplex.*;
import utils.Matrix;
import utils.Pair;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConnectCallbackSolver implements Closeable {
    @Override
    public void close() {
        log.close();
    }

    // data class:

    private static class Variables {
        public final List<IloNumVar> a;
        public final List<IloNumVar> f;
        public final List<IloNumVar> g;
        public final List<IloNumVar> alpha;
        public final List<IloNumVar> beta;

        public final List<IloNumVar> r;
        public final List<IloNumVar> q;

        public final List<IloNumVar> x;

        public final List<IloNumVar> s;
        public final List<IloNumVar> t;

        public final List<IloNumVar> y;

        public Variables() {
            this.a = new ArrayList<>();
            this.f = new ArrayList<>();
            this.g = new ArrayList<>();
            this.alpha = new ArrayList<>();
            this.beta = new ArrayList<>();

            this.r = new ArrayList<>();
            this.q = new ArrayList<>();

            this.x = new ArrayList<>();

            this.s = new ArrayList<>();
            this.t = new ArrayList<>();

            this.y = new ArrayList<>();
        }
    }

    // constants:

    private final static float INF = 1000;
    private final static int TIME_LIMIT = 20;
    private final static int L1NORM = 250;
    private final static double STEP = 0.001;

    // variables:

    private final PrintWriter log;

    private final Matrix matrix;
    private final int D;
    private final int N;

    private final Graph graph;
    private final int E;

    private final Variables v;

    private final IloCplex cplex;

    private int cnt_ans = 0;

    // constructor:

    public ConnectCallbackSolver(Matrix matrix, Graph graph) throws IloException, IOException {
        this.log = new PrintWriter("./logs/connect_callback_solver.txt", StandardCharsets.UTF_8);

        this.matrix = matrix;
        this.N = matrix.numRows();
        this.D = matrix.numCols();

        this.graph = graph;
        this.E = graph.getEdges().size();

        if (graph.getNodesCount() != N) {
            throw new RuntimeException("vertex count not equals with row count");
        }

        this.v = new Variables();

        this.cplex = new IloCplex();
        this.cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);
        this.cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT);

        addVariables();
        addObjective();
        addConstraint();
        addConnectConstraint();

        tuning();
    }

    // private methods:

    private void addVariables() throws IloException {
        for (int i = 0; i < D; i++) {
            v.a.add(cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("a", i)));
        }
        for (int i = 0; i < N; i++) {
            v.f.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("f", i)));
            v.g.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("g", i)));
            v.alpha.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("alpha", i)));
            v.beta.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("beta", i)));
        }
        for (int i = 0; i < N; i++) {
            v.r.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("r", i)));
            v.q.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("q", i)));
            v.s.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("s", i)));
            v.t.add(cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("t", i)));
        }
        for (int i = 0; i < E; i++) {
            v.x.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("x", i)));
            v.y.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("y", i)));
        }
    }

    private void addObjective() throws IloException {
        IloNumExpr[] squares = new IloNumExpr[D];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = cplex.prod(cplex.prod(v.a.get(i), v.a.get(i)), N * 10);
        }
        IloNumExpr[] err_f = new IloNumExpr[N];
        for (int i = 0; i < err_f.length; i++) {
            err_f[i] = cplex.diff(v.f.get(i), v.q.get(i));
            err_f[i] = cplex.prod(err_f[i], err_f[i]);
        }
        IloNumExpr[] err_g = new IloNumExpr[N];
        for (int i = 0; i < err_g.length; i++) {
            err_g[i] = cplex.diff(v.g.get(i), v.t.get(i));
            err_g[i] = cplex.prod(err_g[i], err_g[i]);
        }
        cplex.addMaximize(cplex.diff(cplex.diff(cplex.sum(squares), cplex.sum(err_f)), cplex.sum(err_g)));
    }

    private static double calcObjective(RawSolution sol) {
        double sum = 0;
        double[] squares = new double[sol.matrix.numCols()];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = sol.a[i] * sol.a[i] * sol.matrix.numRows() * 10;
            sum += squares[i];
        }
        double[] err_f = new double[sol.matrix.numRows()];
        for (int i = 0; i < err_f.length; i++) {
            err_f[i] = (sol.f[i] - sol.q[i]);
            err_f[i] = err_f[i] * err_f[i];
            sum -= err_f[i];
        }
        double[] err_g = new double[sol.matrix.numRows()];
        for (int i = 0; i < err_g.length; i++) {
            err_g[i] = (sol.g[i] - sol.t[i]);
            err_g[i] = err_g[i] * err_g[i];
            sum -= err_g[i];
        }
        return sum;
    }

    private void addConstraint() throws IloException {
        for (int i = 0; i < N; i++) {
            cplex.addEq(
                    cplex.scalProd(matrix.getRow(i), toArray(v.a)),
                    cplex.diff(v.f.get(i), v.g.get(i))
            );
        }

        IloNumExpr[] l1normP = new IloNumExpr[N];
        for (int i = 0; i < l1normP.length; i++) {
            l1normP[i] = cplex.sum(v.f.get(i), v.g.get(i));
        }
        cplex.addEq(cplex.sum(l1normP), L1NORM);

        for (int i = 0; i < N; i++) {
            cplex.addLe(v.f.get(i), cplex.prod(v.alpha.get(i), INF));
            cplex.addLe(v.g.get(i), cplex.prod(v.beta.get(i), INF));
            cplex.addEq(cplex.sum(v.alpha.get(i), v.beta.get(i)), 1);
        }
    }

    private void addConnectConstraint() throws IloException {
        cplex.addEq(cplex.sum(toArray(v.r)), 1);
        cplex.addEq(cplex.sum(toArray(v.s)), 1);

        for (int num = 0; num < E; num += 2) {
            int back_num = Graph.companionEdge(num);
            if (back_num != num + 1) {
                throw new RuntimeException("unexpected back_num");
            }

            Graph.checkEdges(graph, num, back_num);

            cplex.addLe(
                    cplex.sum(v.x.get(num), v.x.get(back_num)),
                    1
            );
            cplex.addLe(
                    cplex.sum(v.y.get(num), v.y.get(back_num)),
                    1
            );
        }

        for (int vertex = 0; vertex < N; vertex++) {
            List<IloNumVar> input_edges_x = new ArrayList<>();
            List<IloNumVar> input_edges_y = new ArrayList<>();
            for (Pair<Integer, Long> to : graph.edgesOf(vertex)) {
                int num = to.second.intValue();
                int back_num = Graph.companionEdge(num);

                Graph.checkEdges(graph, num, back_num);
                Graph.checkDest(graph, back_num, vertex);

                input_edges_x.add(v.x.get(back_num));
                input_edges_y.add(v.y.get(back_num));
            }

            cplex.addEq(
                    cplex.sum(cplex.sum(toArray(input_edges_x)), v.r.get(vertex)),
                    1
            );
            cplex.addEq(
                    cplex.sum(cplex.sum(toArray(input_edges_y)), v.s.get(vertex)),
                    1
            );
        }

        for (int num = 0; num < E; num++) {
            Pair<Integer, Integer> edge = graph.getEdges().get(num);
            cplex.addGe(
                    cplex.sum(INF, cplex.diff(v.q.get(edge.first), v.q.get(edge.second))),
                    cplex.sum(cplex.prod(INF, v.x.get(num)), STEP)
            );
            cplex.addGe(
                    cplex.sum(INF, cplex.diff(v.t.get(edge.first), v.t.get(edge.second))),
                    cplex.sum(cplex.prod(INF, v.y.get(num)), STEP)
            );
        }
    }

    private void tuning() throws IloException {
        cplex.use(new ICACallback());
    }

    // callback:

    private record RawSolution(
            IloNumVar[] vars,
            Matrix matrix,
            Graph graph,
            double[] a,
            double[] f,
            double[] g,
            double[] alpha,
            double[] beta,
            double[] r,
            double[] q,
            double[] x,
            double[] s,
            double[] t,
            double[] y
    ) {
        private static final double eps = 1e-6;

        public boolean adapt() {
            double[] p = mul(matrix, a);

            double l1norm = calcL1Norm(p);

            if (l1norm < 0.1) {
                return false;
            }

            double cff = 1;
            if (Math.abs(l1norm - L1NORM) > eps) {
                cff = L1NORM / l1norm;
            }

            for (int i = 0; i < a.length; i++) {
                a[i] *= cff;
            }

            double[] new_p = mul(matrix, a);

            for (int i = 0; i < new_p.length; i++) {
                if (new_p[i] > 0) {
                    f[i] = Math.abs(new_p[i]);
                    g[i] = 0;
                    alpha[i] = 1;
                    beta[i] = 0;
                } else {
                    f[i] = 0;
                    g[i] = Math.abs(new_p[i]);
                    alpha[i] = 0;
                    beta[i] = 1;
                }
            }

            if (Math.abs(calcL1Norm(new_p) - L1NORM) > eps) {
                throw new RuntimeException("unexpected l1norm after adapt");
            }

            for (int i = 0; i < f.length; i++) {
                q[i] = f[i];
            }

            for (int i = 0; i < g.length; i++) {
                t[i] = g[i];
            }

            MST.solve(graph, x, q, r, STEP);

            MST.solve(graph, y, t, s, STEP);

            return true;
        }

        private static double calcL1Norm(double[] p) {
            double l1norm = 0;
            for (double val : p) {
                l1norm += Math.abs(val);
            }
            return l1norm;
        }

        private static double[] mul(Matrix matrix, double[] a) {
            return matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);
        }

        public double[] getValues() {
            double[] ans = new double[matrix.numCols() + 8 * matrix.numRows() + 2 * graph().getEdges().size()];
            int ind_ans = 0;
            for (double z : a) ans[ind_ans++] = z;
            for (double z : f) ans[ind_ans++] = z;
            for (double z : g) ans[ind_ans++] = z;
            for (double z : alpha) ans[ind_ans++] = z;
            for (double z : beta) ans[ind_ans++] = z;
            for (double z : r) ans[ind_ans++] = z;
            for (double z : q) ans[ind_ans++] = z;
            for (double z : x) ans[ind_ans++] = z;
            for (double z : s) ans[ind_ans++] = z;
            for (double z : t) ans[ind_ans++] = z;
            for (double z : y) ans[ind_ans++] = z;
            return ans;
        }

        @Override
        public String toString() {
            return "RawSolution{" +
                    "\n| l1norm = " + calcL1Norm(mul(matrix, a)) +
                    "\n| obj = " + calcObjective(this) +
                    "\n| a = " + Arrays.toString(a) +
                    "\n| f = " + Arrays.toString(f) +
                    "\n| g = " + Arrays.toString(g) +
                    "\n| alpha = " + Arrays.toString(alpha) +
                    "\n| beta = " + Arrays.toString(beta) +
                    "\n| r = " + Arrays.toString(r) +
                    "\n| q = " + Arrays.toString(q) +
                    "\n| x = " + Arrays.toString(x) +
                    "\n| s = " + Arrays.toString(s) +
                    "\n| t = " + Arrays.toString(t) +
                    "\n| y = " + Arrays.toString(y) +
                    "\n}";
        }
    }

    private class ICACallback extends IloCplex.HeuristicCallback {
        @Override
        protected void main() throws IloException {
            IloNumVar[] vars = new IloNumVar[D + 8 * N + 2 * E];
            int ind_var = 0;
            for (IloNumVar z : v.a) vars[ind_var++] = z;
            for (IloNumVar z : v.f) vars[ind_var++] = z;
            for (IloNumVar z : v.g) vars[ind_var++] = z;
            for (IloNumVar z : v.alpha) vars[ind_var++] = z;
            for (IloNumVar z : v.beta) vars[ind_var++] = z;
            for (IloNumVar z : v.r) vars[ind_var++] = z;
            for (IloNumVar z : v.q) vars[ind_var++] = z;
            for (IloNumVar z : v.x) vars[ind_var++] = z;
            for (IloNumVar z : v.s) vars[ind_var++] = z;
            for (IloNumVar z : v.t) vars[ind_var++] = z;
            for (IloNumVar z : v.y) vars[ind_var++] = z;

            RawSolution sol = new RawSolution(
                    vars,
                    matrix,
                    graph,
                    this.getValues(toArray(v.a)),
                    this.getValues(toArray(v.f)),
                    this.getValues(toArray(v.g)),
                    this.getValues(toArray(v.alpha)),
                    this.getValues(toArray(v.beta)),
                    this.getValues(toArray(v.r)),
                    this.getValues(toArray(v.q)),
                    this.getValues(toArray(v.x)),
                    this.getValues(toArray(v.s)),
                    this.getValues(toArray(v.t)),
                    this.getValues(toArray(v.y))
            );

            String oldStr = sol.toString();

            if (sol.adapt()) {

                String newStr = sol.toString();

                double calcObj = calcObjective(sol);

                log.println(cnt_ans);
                log.println("before: " + oldStr);
                log.println("after: " + newStr);
                log.println();

                try {
                    try (PrintWriter out_q = new PrintWriter("./answers/q.txt")) {
                        for (int i = 0; i < sol.q.length; i++) {
                            out_q.println(sol.q[i]);
                        }
                    }
                    try (PrintWriter out_x = new PrintWriter("./answers/x.txt")) {
                        for (int i = 0; i < sol.x.length; i++) {
                            out_x.println(sol.x[i]);
                        }
                    }
                    try (PrintWriter out_t = new PrintWriter("./answers/t.txt")) {
                        for (int i = 0; i < sol.t.length; i++) {
                            out_t.println(sol.t[i]);
                        }
                    }
                    try (PrintWriter out_y = new PrintWriter("./answers/y.txt")) {
                        for (int i = 0; i < sol.y.length; i++) {
                            out_y.println(sol.y[i]);
                        }
                    }
                    DrawUtils.readResultAndDrawAll("./answers/", "tmp_ans" + cnt_ans++, graph);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (calcObj >= getIncumbentObjValue()) {
                    //System.out.println("found new solution: " + calcObj);
                    setSolution(sol.vars(), sol.getValues());
                }

            }
        }
    }


    // public methods:

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    public void writeVarsToFiles(PrintWriter out_q, PrintWriter out_x, PrintWriter out_t, PrintWriter out_y) throws IloException {
//        System.out.println("obj = " + cplex.getObjValue());
//        for (int i = 0; i < D; i++) {
//            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(v.a.get(i)));
//        }
        // to file:
        for (int i = 0; i < v.q.size(); i++) {
            out_q.println(cplex.getValue(v.q.get(i)));
        }
        for (int i = 0; i < v.x.size(); i++) {
            out_x.println(cplex.getValue(v.x.get(i)));
        }
        for (int i = 0; i < v.t.size(); i++) {
            out_t.println(cplex.getValue(v.t.get(i)));
        }
        for (int i = 0; i < v.y.size(); i++) {
            out_y.println(cplex.getValue(v.y.get(i)));
        }
    }

    // private static methods:

    private static String varNameOf(String arg1, int arg2) {
        return arg1 + arg2;
    }

    private static IloNumVar[] toArray(List<IloNumVar> arg) {
        IloNumVar[] result = new IloNumVar[arg.size()];
        for (int i = 0; i < arg.size(); i++) {
            result[i] = arg.get(i);
        }
        return result;
    }

}
