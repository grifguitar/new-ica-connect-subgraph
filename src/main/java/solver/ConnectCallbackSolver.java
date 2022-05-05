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

        public Variables() {
            this.a = new ArrayList<>();
            this.f = new ArrayList<>();
            this.g = new ArrayList<>();
            this.alpha = new ArrayList<>();
            this.beta = new ArrayList<>();

            this.r = new ArrayList<>();
            this.q = new ArrayList<>();

            this.x = new ArrayList<>();
        }
    }

    // constants:

    private final static float INF = 1000;
    private final static int TIME_LIMIT = 20;
    private final static int L1NORM = 250;
    private final static double PHI = 0.01;
    private final static double STEP = 0.01;

    // variables:

    private final PrintWriter log;

    private final Matrix matrix;
    private final int D;
    private final int N;

    private final Graph graph;
    private final int E;

    private final Variables v;

    private final IloCplex cplex;

    //private final AtomicReference<Double> lb;

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

        //this.lb = new AtomicReference<>(-1e10);

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
        }
        for (int i = 0; i < E; i++) {
            v.x.add(cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("x", i)));
        }
    }

    private void addObjective() throws IloException {
        IloNumExpr[] squares = new IloNumExpr[D];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = cplex.prod(v.a.get(i), v.a.get(i));
        }
        IloNumExpr[] err = new IloNumExpr[N];
        for (int i = 0; i < err.length; i++) {
            err[i] = cplex.diff(v.f.get(i), v.q.get(i));
            err[i] = cplex.prod(err[i], err[i]);
        }
        cplex.addMaximize(cplex.diff(cplex.sum(squares), cplex.sum(err)));
    }

    private static double calcObjective(RawSolution sol) {
        double sum = 0;
        double[] squares = new double[sol.matrix.numCols()];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = sol.a[i] * sol.a[i];
            sum += squares[i];
        }
        double[] err = new double[sol.matrix.numRows()];
        for (int i = 0; i < err.length; i++) {
            err[i] = (sol.f[i] - sol.q[i]);
            err[i] = err[i] * err[i];
            sum -= err[i];
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
        }

        for (int vertex = 0; vertex < N; vertex++) {
            List<IloNumVar> input_edges = new ArrayList<>();
            for (Pair<Integer, Long> to : graph.edgesOf(vertex)) {
                int num = to.second.intValue();
                int back_num = Graph.companionEdge(num);

                Graph.checkEdges(graph, num, back_num);
                Graph.checkDest(graph, back_num, vertex);

                input_edges.add(v.x.get(back_num));
            }

            cplex.addEq(
                    cplex.sum(cplex.sum(toArray(input_edges)), v.r.get(vertex)),
                    1
            );
        }

        for (int num = 0; num < E; num++) {
            Pair<Integer, Integer> edge = graph.getEdges().get(num);
            cplex.addLe(
                    cplex.prod(PHI, v.x.get(num)),
                    v.q.get(edge.first)
            );
            cplex.addLe(
                    cplex.prod(PHI, v.x.get(num)),
                    v.q.get(edge.second)
            );
            cplex.addGe(
                    cplex.sum(INF, cplex.diff(v.q.get(edge.first), v.q.get(edge.second))),
                    cplex.sum(cplex.prod(INF, v.x.get(num)), STEP)
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
            double[] x
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

            MST.solve(graph, x, q, r, STEP);

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
            double[] ans = new double[matrix.numCols() + 6 * matrix.numRows() + graph().getEdges().size()];
            int ind_ans = 0;
            for (double y : a) ans[ind_ans++] = y;
            for (double y : f) ans[ind_ans++] = y;
            for (double y : g) ans[ind_ans++] = y;
            for (double y : alpha) ans[ind_ans++] = y;
            for (double y : beta) ans[ind_ans++] = y;
            for (double y : r) ans[ind_ans++] = y;
            for (double y : q) ans[ind_ans++] = y;
            for (double y : x) ans[ind_ans++] = y;
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
                    "\n}";
        }
    }

    private class ICACallback extends IloCplex.HeuristicCallback {
        @Override
        protected void main() throws IloException {
            IloNumVar[] vars = new IloNumVar[D + 6 * N + E];
            int ind_var = 0;
            for (IloNumVar y : v.a) vars[ind_var++] = y;
            for (IloNumVar y : v.f) vars[ind_var++] = y;
            for (IloNumVar y : v.g) vars[ind_var++] = y;
            for (IloNumVar y : v.alpha) vars[ind_var++] = y;
            for (IloNumVar y : v.beta) vars[ind_var++] = y;
            for (IloNumVar y : v.r) vars[ind_var++] = y;
            for (IloNumVar y : v.q) vars[ind_var++] = y;
            for (IloNumVar y : v.x) vars[ind_var++] = y;

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
                    this.getValues(toArray(v.x))
            );

            String oldStr = sol.toString();

            if (sol.adapt()) {

                String newStr = sol.toString();

                double calcObj = calcObjective(sol);

                //if (calcObj > lb.get()) {
                //    lb.set(calcObj);
                //}
                log.println("before: " + oldStr);
                log.println("after: " + newStr);
                log.println();

                try {
                    try (PrintWriter output = new PrintWriter(
                            "./answers/p.txt",
                            StandardCharsets.UTF_8)
                    ) {
                        for (int i = 0; i < N; i++) {
                            output.println(sol.q[i]);
                        }
                    }
                    DrawUtils.drawingAnswer("./answers/", "tmp_ans" + cnt_ans++);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                //}

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

    public void printResults(PrintWriter out) throws IloException {
        System.out.println("obj = " + cplex.getObjValue());
        for (int i = 0; i < D; i++) {
            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(v.a.get(i)));
        }
        for (int i = 0; i < N; i++) {
            //System.out.println(varNameOf("p", i) + " = " + cplex.getValue(var.P.get(i)));
            //double p0 = cplex.getValue(v.g.get(i)) - cplex.getValue(v.f.get(i));
            double p0 = cplex.getValue(v.q.get(i));
            out.println(p0);
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
