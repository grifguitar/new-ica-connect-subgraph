import drawing.DrawAPI;
import drawing.DrawUtils;
import graph.Graph;
import io.GraphIO;
import io.NewMatrixIO;
import solver.ConnectCallbackSolver;
import utils.Matrix;
import utils.Pair;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static analysis.DataAnalysis.whitening;

public class Main {
    private static final String OUT_N = "./answers/p_ans_";
    private static final String IN = "./input_data/";
    private static final String LOGS = "./logs/";
    private static final String FILENAME = "1_test_small_025";
    private static final String OUT_NET_CL_N = "./answers/net_cl_ans_";

    public static void main(String[] args) {
        try {
            // create net_clust predict answers file from .netclust_ans

            Map<String, Integer> namingMapAnsNetCl = new HashMap<>();
            Map<Integer, String> revNamingMapAnsNetCl = new HashMap<>();

            Matrix ansNetCl = NewMatrixIO.read(IN + FILENAME + ".netclust_ans", true, namingMapAnsNetCl, revNamingMapAnsNetCl);

            for (int w = 0; w < ansNetCl.numCols(); w++) {
                try (PrintWriter out = new PrintWriter(OUT_NET_CL_N + w + ".txt")) {
                    for (int i = 0; i < ansNetCl.numRows(); i++) {
                        out.println(ansNetCl.getElem(i, w));
                    }
                }
            }

            // create true answers file from .ans

            Map<String, Integer> namingMapAns = new HashMap<>();
            Map<Integer, String> revNamingMapAns = new HashMap<>();

            Matrix ans = NewMatrixIO.read(IN + FILENAME + ".ans", true, namingMapAns, revNamingMapAns);

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

            Matrix matrix = NewMatrixIO.read(IN + FILENAME + ".mtx", true, namingMap, revNamingMap);

            // read graph

            Graph graph = GraphIO.read(IN + FILENAME + ".graph", namingMap, revNamingMap);

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
            if (namingMapAnsNetCl.size() != namingMapAns.size()) {
                throw new RuntimeException("not equals naming map");
            }
            namingMapAnsNetCl.forEach((k, v) -> {
                if (!Objects.equals(namingMapAns.get(k), v)) {
                    throw new RuntimeException("not equals naming map");
                }
            });

            // whitening

            matrix = whitening(matrix);

            // solve

            ConnectCallbackSolver solver = new ConnectCallbackSolver(matrix, graph);
            //SimpleSolver solver = new SimpleSolver(matrix);

            if (solver.solve()) {
                try (PrintWriter out_q = new PrintWriter("./answers/q.txt")) {
                    try (PrintWriter out_x = new PrintWriter("./answers/x.txt")) {
                        try (PrintWriter out_t = new PrintWriter("./answers/t.txt")) {
                            try (PrintWriter out_y = new PrintWriter("./answers/y.txt")) {
                                solver.writeVarsToFiles(out_q, out_x, out_t, out_y);
                            }
                        }
                    }
                }
                DrawUtils.readResultAndDrawAll("./answers/", "total_ans", graph);
            } else {
                System.out.println("ConnectCallbackSolver: integer results not found!");
            }

            solver.close();

            DrawUtils.compareNetClustWithTrueAns("./answers/", "net_clust");

            DrawAPI.run();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
