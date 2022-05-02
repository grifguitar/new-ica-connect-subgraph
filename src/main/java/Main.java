import drawing.DrawAPI;
import drawing.DrawUtils;
import io.MatrixIO;
import solver.SimpleSolver;
import utils.Matrix;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static analysis.DataAnalysis.whitening;

public class Main {
    private static final String OUT = "./answers/p.txt";
    private static final String OUT_N = "./answers/p_ans_";
    private static final String IN = "./input_data/";

    public static void main(String[] args) {
        try {
            // create true answers file from .ans

            Matrix ans = MatrixIO.read(IN + "test_small_025.ans", true, null, null);

            for (int w = 0; w < ans.numCols(); w++) {
                try (PrintWriter out = new PrintWriter(OUT_N + w + ".txt")) {
                    for (int i = 0; i < ans.numRows(); i++) {
                        out.println(ans.getElem(i, w));
                    }
                }
            }

            // calculate

            Map<Integer, String> rev_map = new HashMap<>();
            Map<String, Integer> map = new HashMap<>();

            Matrix matrix = MatrixIO.read(IN + "test_small_025.mtx", true, rev_map, map);

            matrix = whitening(matrix);

            SimpleSolver solver = new SimpleSolver(matrix);

            if (solver.solve()) {
                try (PrintWriter out = new PrintWriter(OUT)) {
                    solver.printResults(out);
                }
            } else {
                System.out.println("debug: results not found!");
            }

            // draw all from folder

            DrawUtils.drawingAnswer("./answers/", "total_answer");
            DrawAPI.run();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
