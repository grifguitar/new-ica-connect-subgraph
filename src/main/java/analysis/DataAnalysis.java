package analysis;

import utils.Matrix;
import utils.Pair;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static utils.Matrix.scalProd;

/**
 * static class
 */
public class DataAnalysis {

    /**
     * private constants
     */
    private static final double EPS = 1e-6;
    private static final String F_DEBUG = "./logs/data_analysis_debug.txt";
    private static final String F_OUT = "./logs/data_analysis_out.txt";

    /**
     * public static methods
     */

    public static Matrix whitening(Matrix matrix) {
        try {
            try (PrintWriter out = new PrintWriter(F_OUT, StandardCharsets.UTF_8)) {
                try (PrintWriter debug = new PrintWriter(F_DEBUG, StandardCharsets.UTF_8)) {

                    Matrix st_mtx = DataAnalysis.standardization(matrix, debug, true, true);

                    Matrix cov_mtx = DataAnalysis.getCovMatrix(st_mtx, debug, true);
                    debug.println("cov_mtx:");
                    debug.println(cov_mtx);

                    Matrix cov_mtx_2 = DataAnalysis.getCovMatrix2(st_mtx);
                    debug.println("cov_mtx_2:");
                    debug.println(cov_mtx_2);

                    Matrix pca_mtx = DataAnalysis.pca(st_mtx, debug, 3);

                    Matrix cov_mtx_pca_mtx = DataAnalysis.getCovMatrix(pca_mtx, debug, true);
                    debug.println("cov_mtx_pca_mtx:");
                    debug.println(cov_mtx_pca_mtx);

                    Matrix final_mtx = DataAnalysis.standardization(pca_mtx, debug, true, true);
                    out.println("final_mtx:");
                    out.println(final_mtx);

                    Matrix cov_final_mtx = DataAnalysis.getCovMatrix(final_mtx, debug, true);
                    debug.println("cov_final_mtx:");
                    debug.println(cov_final_mtx);

                    return final_mtx;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Matrix standardization(Matrix matrix, PrintWriter out, boolean DEBUG, boolean deviation) {
        int components = matrix.numCols();
        int rows = matrix.numRows();

        double[][] res = new double[rows][components];

        if (DEBUG) {
            out.println("# components number: " + components);
        }

        for (int component = 0; component < components; component++) {
            double E = 0;

            for (int row = 0; row < rows; row++) {
                E += matrix.getElem(row, component);
            }

            E /= rows;

            double D = 0;

            for (int row = 0; row < rows; row++) {
                res[row][component] = matrix.getElem(row, component) - E;
                D += res[row][component] * res[row][component];
            }

            D /= rows;

            double st_dev = Math.sqrt(D);

            if (deviation) {
                for (int row = 0; row < rows; row++) {
                    res[row][component] /= st_dev;
                }
            }

            if (DEBUG) {
                out.println();
                out.println("# current component: " + component);
                out.println("# E: " + E + " ; D: " + D + " ; st_dev: " + st_dev);
            }

        }

        return new Matrix(res);
    }

    public static Matrix getCovMatrix(Matrix matrix, PrintWriter out, boolean DEBUG) {
        int components = matrix.numCols();
        int rows = matrix.numRows();

        if (DEBUG) {
            out.println("# components number: " + components);
        }

        double[] EE = new double[components];

        for (int component = 0; component < components; component++) {
            EE[component] = 0;

            for (int row = 0; row < rows; row++) {
                EE[component] += matrix.getElem(row, component);
            }

            EE[component] /= rows;
        }

        for (double E : EE) {
            if (E > EPS) {
                throw new RuntimeException("expected zero - mean!");
            }
        }

        if (DEBUG) {
            out.print("# EE: (must be zero values): ");
            for (int i = 0; i < components; i++) {
                out.print(EE[i]);
                out.print(", ");
            }
            out.println();
        }

        double[][] cov_matrix = new double[components][components];

        for (int c1 = 0; c1 < components; c1++) {
            for (int c2 = 0; c2 < components; c2++) {

                cov_matrix[c1][c2] = 0;

                for (int row = 0; row < rows; row++) {
                    cov_matrix[c1][c2] += matrix.getElem(row, c1) * matrix.getElem(row, c2);
                }

                cov_matrix[c1][c2] /= rows;

            }
        }

        return new Matrix(cov_matrix);
    }

    public static Matrix getCovMatrix2(Matrix matrix) {
        int N = matrix.numRows();
        Matrix matrix2 = matrix.transpose();
        return matrix2.mult(matrix).div(N);
    }

    public static Matrix pca(Matrix matrix, PrintWriter err, int R) {
        Matrix cov_matrix = getCovMatrix2(matrix);

        Pair<double[], double[][]> decompose = cov_matrix.decomposition(err);

        err.println("# pca debug: ");
        err.println(Arrays.toString(decompose.first));
        for (int i = 0; i < R; i++) {
            err.println("# eigen " + i + ":");
            err.println(decompose.first[i]);
            err.println(Arrays.toString(decompose.second[i]));
        }
        err.println();

        double[][] newData = new double[matrix.numRows()][R];

        for (int i = 0; i < matrix.numRows(); i++) {
            for (int j = 0; j < R; j++) {
                newData[i][j] = scalProd(matrix.getRow(i), decompose.second[j]);
            }
        }

        return new Matrix(newData);
    }

}
