package utils;

import org.apache.commons.math3.linear.*;

import java.io.PrintWriter;
import java.util.*;

public class Matrix {
    private static final double EPS = 1e-6;

    private final RealMatrix entry;

    public Matrix(double[][] data) {
        this.entry = new Array2DRowRealMatrix(data);
    }

    public Matrix(double[] vec) {
        this.entry = new Array2DRowRealMatrix(new double[][]{vec});
    }

    public Matrix(List<List<Double>> data) {
        if (data.isEmpty()) {
            throw new RuntimeException("expected non-empty matrix argument");
        }
        double[][] this_data = new double[data.size()][data.get(0).size()];
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).size() != data.get(0).size()) {
                throw new RuntimeException("wrong matrix argument, no rectangle");
            }
            for (int j = 0; j < data.get(0).size(); j++) {
                this_data[i][j] = data.get(i).get(j);
            }
        }
        this.entry = new Array2DRowRealMatrix(this_data);
    }

    public Matrix(RealMatrix mtx) {
        this.entry = mtx;
    }

    public int numCols() {
        return entry.getColumnDimension();
    }

    public int numRows() {
        return entry.getRowDimension();
    }

    public double[] getRow(int row) {
        return entry.getRow(row);
    }

    public double getElem(int row, int col) {
        return entry.getEntry(row, col);
    }

    public Matrix transpose() {
        return new Matrix(this.entry.transpose());
    }

    public Matrix mult(Matrix other) {
        return new Matrix(this.entry.multiply(other.entry));
    }

    public Matrix div(double N) {
        double[][] res = new double[numRows()][numCols()];
        for (int row = 0; row < numRows(); row++) {
            for (int col = 0; col < numCols(); col++) {
                res[row][col] = getElem(row, col) / N;
            }
        }
        return new Matrix(res);
    }

    public Pair<double[], double[][]> decomposition(PrintWriter err) {
        if (numRows() != numCols()) {
            throw new RuntimeException("expected square matrix!");
        }

        for (int i = 0; i < numRows(); i++) {
            for (int j = 0; j < numCols(); j++) {
                if (getElem(i, j) != getElem(j, i)) {
                    throw new RuntimeException("expected symmetric matrix!");
                }
            }
        }

        EigenDecomposition eig = new EigenDecomposition(entry);

        if (eig.hasComplexEigenvalues()) {
            throw new RuntimeException("expected non-complex eigen values!");
        }

        double[] eigenValues = eig.getRealEigenvalues();

        if (eigenValues.length != numRows()) {
            throw new RuntimeException("expected eigen values count of N!");
        }

        for (int i = 0; i < eigenValues.length - 1; i++) {
            if (eigenValues[i] < eigenValues[i + 1]) {
                throw new RuntimeException("expected sorted eigen values!");
            }
        }

        err.println("# decompose debug:");

        double[][] eigenVectors = new double[numRows()][numRows()];
        for (int i = 0; i < eigenVectors.length; i++) {
            double[] eigenVector = eig.getEigenvector(i).toArray();

            if (eigenVector.length != eigenVectors[i].length) {
                throw new RuntimeException("expected eigenVector length of N");
            }

            double sum = 0;
            for (int j = 0; j < eigenVector.length; j++) {
                eigenVectors[i][j] = eigenVector[j];
                sum += eigenVector[j] * eigenVector[j];
            }

            err.println("!!! eigenVector sum : " + sum + " : " + Arrays.toString(eigenVector));

            if (Math.abs(sum - 1) > EPS) {
                throw new RuntimeException("L2-norm of eigen vector must be 1!");
            }

            System.arraycopy(eigenVector, 0, eigenVectors[i], 0, eigenVector.length);
        }

        return new Pair<>(eigenValues, eigenVectors);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numRows(); i++) {
            for (int j = 0; j < numCols(); j++) {
                sb.append(getElem(i, j));
                sb.append("\t");
            }
            sb.append("\n");
        }
        return "Matrix{\n" + sb + "}\n";
    }

    public static double scalProd(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new RuntimeException("first array length must be equals second array length!");
        }

        double ans = 0;

        for (int i = 0; i < x.length; i++) {
            ans += x[i] * y[i];
        }

        return ans;
    }
}
