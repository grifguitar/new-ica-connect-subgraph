import drawing.DrawAPI;
import drawing.DrawUtils;
import graph.Graph;
import io.GraphIO;
import io.NewMatrixIO;
import solver.SimpleCallbackSolver;
import solver.SimpleSolver;
import utils.Matrix;
import utils.Pair;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static analysis.DataAnalysis.whitening;

public class Main {
    private static final String OUT = "./answers/p.txt";
    private static final String OUT_N = "./answers/p_ans_";
    private static final String IN = "./input_data/";
    private static final String LOGS = "./logs/";

    public static void main(String[] args) {
        try {
            // create true answers file from .ans

            Map<String, Integer> namingMapAns = new HashMap<>();
            Map<Integer, String> revNamingMapAns = new HashMap<>();

            Matrix ans = NewMatrixIO.read(IN + "test_small_025.ans", true, namingMapAns, revNamingMapAns);

            for (int w = 0; w < ans.numCols(); w++) {
                try (PrintWriter out = new PrintWriter(OUT_N + w + ".txt")) {
                    for (int i = 0; i < ans.numRows(); i++) {
                        out.println(ans.getElem(i, w));
                    }
                }
            }

            // read matrix

            Map<String, Integer> namingMap = new HashMap<>();
            Map<Integer, String> revNamingMap = new HashMap<>();

            Matrix matrix = NewMatrixIO.read(IN + "test_small_025.mtx", true, namingMap, revNamingMap);

            // read graph

            Graph graph = GraphIO.read(IN + "test_small_025.graph", namingMap);

            try (PrintWriter log = new PrintWriter(LOGS + "edges.txt")) {
                for (Pair<Integer, Integer> edge : graph.getEdges()) {
                    log.println(revNamingMap.get(edge.first) + "\t" + revNamingMap.get(edge.second));
                }
            }

            // check

            if (namingMap.size() != namingMapAns.size()) {
                throw new RuntimeException("not equals naming map");
            }
            namingMap.forEach((k, v) -> {
                if (!Objects.equals(namingMapAns.get(k), v)) {
                    throw new RuntimeException("not equals naming map");
                }
            });

            // whitening

            matrix = whitening(matrix);

            // solve

            SimpleCallbackSolver solver = new SimpleCallbackSolver(matrix);

            if (solver.solve()) {
                try (PrintWriter out = new PrintWriter(OUT)) {
                    solver.printResults(out);
                }
            } else {
                System.out.println("debug: results not found!");
            }

            solver.close();

            // draw all from folder

            DrawUtils.drawingAnswer("./answers/", "total_answer");
            DrawAPI.run();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
