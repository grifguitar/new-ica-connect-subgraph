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

public class SimpleCallbackSolver implements Closeable {
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

        public Variables() {
            this.a = new ArrayList<>();
            this.f = new ArrayList<>();
            this.g = new ArrayList<>();
            this.alpha = new ArrayList<>();
            this.beta = new ArrayList<>();
        }
    }

    // constants:

    private final static float INF = 1000;

    // variables:

    private final PrintWriter log;

    private final Matrix matrix;
    private final int D;
    private final int N;

    private final Graph graph;
    private final int E;

    private final Variables v;
    private RawSolution best;

    private final IloCplex cplex;

    private int cnt_ans = 0;

    // constructor:

    public SimpleCallbackSolver(Matrix matrix, Graph graph, int TIME_LIMIT) throws IloException, IOException {
        this.log = new PrintWriter("./logs/simple_callback_solver.txt", StandardCharsets.UTF_8);

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
    }

    private void addObjective() throws IloException {
        IloNumExpr[] squares = new IloNumExpr[D];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = cplex.prod(v.a.get(i), v.a.get(i));
        }
        cplex.addMaximize(cplex.sum(squares));
    }

    private static double calcObjective(RawSolution sol) {
        double sum = 0;
        double[] squares = new double[sol.matrix.numCols()];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = sol.a[i] * sol.a[i];
            sum += squares[i];
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
        cplex.addEq(cplex.sum(l1normP), N);

        for (int i = 0; i < N; i++) {
            cplex.addLe(v.f.get(i), cplex.prod(v.alpha.get(i), INF));
            cplex.addLe(v.g.get(i), cplex.prod(v.beta.get(i), INF));
            cplex.addEq(cplex.sum(v.alpha.get(i), v.beta.get(i)), 1);
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
            if (Math.abs(l1norm - matrix.numRows()) > eps) {
                cff = matrix.numRows() / l1norm;
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

            if (Math.abs(calcL1Norm(new_p) - matrix.numRows()) > eps) {
                throw new RuntimeException("unexpected l1norm after adapt");
            }

            for (int i = 0; i < f.length; i++) {
                q[i] = f[i];
            }

            for (int i = 0; i < g.length; i++) {
                t[i] = g[i];
            }

            for (int i = 0; i < graph.getEdges().size(); i++) {
                Pair<Integer, Integer> edge = graph.getEdges().get(i);
                x[i] = q[edge.first] + q[edge.second];
            }

            for (int i = 0; i < graph.getEdges().size(); i++) {
                Pair<Integer, Integer> edge = graph.getEdges().get(i);
                y[i] = t[edge.first] + t[edge.second];
            }

            MST.solve(graph, x, q, r, 0);

            MST.solve(graph, y, t, s, 0);

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
            double[] ans = new double[matrix.numCols() + 4 * matrix.numRows()];
            int ind_ans = 0;
            for (double x : a) ans[ind_ans++] = x;
            for (double x : f) ans[ind_ans++] = x;
            for (double x : g) ans[ind_ans++] = x;
            for (double x : alpha) ans[ind_ans++] = x;
            for (double x : beta) ans[ind_ans++] = x;
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
            IloNumVar[] vars = new IloNumVar[D + 4 * N];
            int ind_var = 0;
            for (IloNumVar x : v.a) vars[ind_var++] = x;
            for (IloNumVar x : v.f) vars[ind_var++] = x;
            for (IloNumVar x : v.g) vars[ind_var++] = x;
            for (IloNumVar x : v.alpha) vars[ind_var++] = x;
            for (IloNumVar x : v.beta) vars[ind_var++] = x;

            RawSolution sol = new RawSolution(
                    vars,
                    matrix,
                    graph,
                    this.getValues(toArray(v.a)),
                    this.getValues(toArray(v.f)),
                    this.getValues(toArray(v.g)),
                    this.getValues(toArray(v.alpha)),
                    this.getValues(toArray(v.beta)),
                    new double[N],
                    new double[N],
                    new double[E],
                    new double[N],
                    new double[N],
                    new double[E]
            );

            String oldStr = sol.toString();

            if (sol.adapt()) {

                String newStr = sol.toString();

                double calcObj = calcObjective(sol);

                cnt_ans++;

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
                    //DrawUtils.newDraw("./answers/", "tmp_ans" + cnt_ans, graph);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (best == null || calcObj >= calcObjective(best)) {
                    best = sol;
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
        System.out.println("obj = " + cplex.getObjValue());
        for (int i = 0; i < D; i++) {
            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(v.a.get(i)));
        }
        // to file:
        for (int i = 0; i < best.q.length; i++) {
            out_q.println(best.q[i]);
        }
        for (int i = 0; i < best.x.length; i++) {
            out_x.println(best.x[i]);
        }
        for (int i = 0; i < best.t.length; i++) {
            out_t.println(best.t[i]);
        }
        for (int i = 0; i < best.y.length; i++) {
            out_y.println(best.y[i]);
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
