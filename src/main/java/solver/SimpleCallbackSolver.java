package solver;

import ilog.concert.*;
import ilog.cplex.*;
import utils.Matrix;

import java.io.PrintWriter;
import java.util.*;

public class SimpleCallbackSolver {

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
    private final static int TIME_LIMIT = 20;
    private final static int L1NORM = 250;

    // variables:

    private final Matrix matrix;
    private final int D;
    private final int N;

    private final Variables v;

    private final IloCplex cplex;

    // constructor:

    public SimpleCallbackSolver(Matrix matrix) throws IloException {
        this.matrix = matrix;
        this.N = matrix.numRows();
        this.D = matrix.numCols();

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
        cplex.addEq(cplex.sum(l1normP), L1NORM);

        for (int i = 0; i < N; i++) {
            cplex.addLe(v.f.get(i), cplex.prod(v.alpha.get(i), INF));
            cplex.addLe(v.g.get(i), cplex.prod(v.beta.get(i), INF));
            cplex.addEq(cplex.sum(v.alpha.get(i), v.beta.get(i)), 1);
        }
    }

    private void tuning() throws IloException {
        //cplex.use(new MIPCallback());
        cplex.use(new ICACallback());
    }

    // callback:

    private record RawSolution(
            IloNumVar[] vars,
            Matrix matrix,
            double[] a,
            double[] f,
            double[] g,
            double[] alpha,
            double[] beta
    ) {
        public boolean adapt() {
            double[] p = mul(matrix, a);

            double l1norm = calcL1Norm(p);

            if (l1norm < 0.1) {
                return false;
            }

            double cff = L1NORM / l1norm;

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

            if (Math.abs(calcL1Norm(new_p) - L1NORM) > 1e-6) {
                throw new RuntimeException("unexpected l1norm after adapt");
            }

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
                    this.getValues(toArray(v.a)),
                    this.getValues(toArray(v.f)),
                    this.getValues(toArray(v.g)),
                    this.getValues(toArray(v.alpha)),
                    this.getValues(toArray(v.beta))
            );

            if (sol.adapt()) {

                double calcObj = calcObjective(sol);

                if (calcObj >= getIncumbentObjValue()) {
                    System.out.println("found new solution: " + calcObj);
                    setSolution(sol.vars(), sol.getValues());
                }

            }
        }
    }

//    private class MIPCallback extends IloCplex.IncumbentCallback {
//        @Override
//        protected void main() throws IloException {
//            double currLB = lb.get();
//            if (currLB < getObjValue()) {
//                if (lb.compareAndSet(currLB, getObjValue())) {
//                    System.out.println("Found new solution: " + getObjValue());
//                }
//            }
//        }
//    }

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
            double p0 = cplex.getValue(v.f.get(i)) - cplex.getValue(v.g.get(i));
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
