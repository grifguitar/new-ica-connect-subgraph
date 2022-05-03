package solver;

import ilog.concert.*;
import ilog.cplex.*;
import utils.Matrix;

import java.io.PrintWriter;
import java.util.*;

public class SimpleSolver {

    // data class:

    private static class Variables {
        public final List<IloNumVar> a;
        public final List<IloNumVar> p;

        public Variables() {
            this.a = new ArrayList<>();
            this.p = new ArrayList<>();
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

    public SimpleSolver(Matrix matrix) throws IloException {
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
    }

    private void addVariables() throws IloException {
        for (int i = 0; i < D; i++) {
            v.a.add(cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("a", i)));
        }
        for (int i = 0; i < N; i++) {
            v.p.add(cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("p", i)));
        }
    }

    private void addObjective() throws IloException {
        IloNumExpr[] squares = new IloNumExpr[D];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = cplex.prod(v.a.get(i), v.a.get(i));
        }
        cplex.addMaximize(cplex.sum(squares));
    }

    private void addConstraint() throws IloException {
        for (int i = 0; i < N; i++) {
            cplex.addEq(cplex.scalProd(matrix.getRow(i), toArray(v.a)), v.p.get(i));
        }

        IloNumExpr[] l1normP = new IloNumExpr[N];
        for (int i = 0; i < l1normP.length; i++) {
            l1normP[i] = cplex.abs(v.p.get(i));
        }
        cplex.addEq(cplex.sum(l1normP), L1NORM);
    }

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    public void printResults(PrintWriter out) throws IloException {
        System.out.println("obj = " + cplex.getObjValue());
        for (int i = 0; i < v.a.size(); i++) {
            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(v.a.get(i)));
        }
        for (int i = 0; i < v.p.size(); i++) {
            //System.out.println(varNameOf("p", i) + " = " + cplex.getValue(var.P.get(i)));
            double p0 = cplex.getValue(v.p.get(i));
            out.println(p0);
        }
    }

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
