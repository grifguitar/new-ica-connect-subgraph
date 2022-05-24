import drawing.DrawAPI;
import drawing.DrawUtils;
import graph.Graph;
import io.GraphIO;
import io.NewMatrixIO;
import solver.ConnectCallbackSolver;
import solver.SimpleCallbackSolver;
import utils.Matrix;
import utils.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static analysis.DataAnalysis.whitening;

public class Main {
    private static final String OUT_FOLDER = "./answers/";
    private static final String OUT_N = "./answers/p_ans_";
    private static final String IN = "./real_data/";
    private static final String LOGS = "./logs/";
    private static final String FILENAME = "real_test_new";
    private static final int ANS_FILES_COUNT = 0;
    private static final boolean REAL_DATA = true;

    public static void main(String[] args) {
        try {
            // create net_clust predict answers file from .netclust_ans

            Map<String, Integer> namingMapAnsNetCl = new HashMap<>();
            Map<Integer, String> revNamingMapAnsNetCl = new HashMap<>();

            List<Integer> sizes = new ArrayList<>();
            for (int cnt = 1; cnt <= ANS_FILES_COUNT; cnt++) {

                namingMapAnsNetCl = new HashMap<>();
                revNamingMapAnsNetCl = new HashMap<>();

                Matrix ansNetCl = NewMatrixIO.read(IN + FILENAME + ".netclust_" + cnt + "_ans", true, namingMapAnsNetCl, revNamingMapAnsNetCl);

                sizes.add(ansNetCl.numCols());
                for (int w = 0; w < ansNetCl.numCols(); w++) {
                    try (PrintWriter out = new PrintWriter(OUT_FOLDER + cnt + "_nc_ans_" + w + ".txt")) {
                        for (int i = 0; i < ansNetCl.numRows(); i++) {
                            out.println(ansNetCl.getElem(i, w));
                        }
                    }
                }

            }
            try (PrintWriter out = new PrintWriter(OUT_FOLDER + "0_clust_size.txt")) {
                for (int size : sizes)
                    out.println(size);
            }

            // read matrix

            Map<String, Integer> namingMap = new HashMap<>();
            Map<Integer, String> revNamingMap = new HashMap<>();

            Matrix matrix = NewMatrixIO.read(IN + FILENAME + ".mtx", true, namingMap, revNamingMap);

            // read hyp ans

            if (REAL_DATA) {
                try (PrintWriter ans_out = new PrintWriter(IN + FILENAME + ".ans", StandardCharsets.UTF_8)) {

                    BufferedReader arg0 = new BufferedReader(new FileReader(IN + FILENAME + ".hyp", StandardCharsets.UTF_8));
                    Set<String> hyp_set = arg0.lines().collect(Collectors.toSet());

                    Scanner scanner1 = new Scanner(new FileReader(IN + FILENAME + ".mtx", StandardCharsets.UTF_8));
                    while (scanner1.hasNextLine()) {
                        String[] tokens = scanner1.nextLine().split("\\s");
                        if (hyp_set.contains(tokens[0])) {
                            ans_out.println(tokens[0] + "\t" + 1);
                        } else {
                            ans_out.println(tokens[0] + "\t" + 0);
                        }
                    }

                } catch (Exception e) {
                    throw new RuntimeException();
                }
            }

            // create true answers file from .ans

            Map<String, Integer> namingMapAns = new HashMap<>();
            Map<Integer, String> revNamingMapAns = new HashMap<>();

            Matrix ans = NewMatrixIO.read(IN + FILENAME + ".ans", true, namingMapAns, revNamingMapAns);

            try (PrintWriter out = new PrintWriter(OUT_FOLDER + "0_module_size.txt")) {
                out.println(ans.numCols());
            }
            for (int w = 0; w < ans.numCols(); w++) {
                try (PrintWriter out = new PrintWriter(OUT_N + w + ".txt")) {
                    for (int i = 0; i < ans.numRows(); i++) {
                        out.println(ans.getElem(i, w));
                    }
                }
            }

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
            if (ANS_FILES_COUNT != 0) {
                if (namingMapAnsNetCl.size() != namingMapAns.size()) {
                    throw new RuntimeException("not equals naming map");
                }
                namingMapAnsNetCl.forEach((k, v) -> {
                    if (!Objects.equals(namingMapAns.get(k), v)) {
                        throw new RuntimeException("not equals naming map");
                    }
                });
            }

            // whitening

            matrix = whitening(matrix);

            // solve

//            ConnectCallbackSolver solver = new ConnectCallbackSolver(matrix, graph, 1000);
//
//            if (solver.solve()) {
//                try (PrintWriter out_q = new PrintWriter("./answers/q.txt")) {
//                    try (PrintWriter out_x = new PrintWriter("./answers/x.txt")) {
//                        try (PrintWriter out_t = new PrintWriter("./answers/t.txt")) {
//                            try (PrintWriter out_y = new PrintWriter("./answers/y.txt")) {
//                                solver.writeVarsToFiles(out_q, out_x, out_t, out_y);
//                            }
//                        }
//                    }
//                }
//            } else {
//                System.out.println("ConnectCallbackSolver: integer results not found!");
//            }
//
//            solver.close();

            SimpleCallbackSolver simpleCallbackSolver = new SimpleCallbackSolver(matrix, graph, 150);

            if (simpleCallbackSolver.solve()) {
                try (PrintWriter out_q = new PrintWriter("./answers/q.txt")) {
                    try (PrintWriter out_x = new PrintWriter("./answers/x.txt")) {
                        try (PrintWriter out_t = new PrintWriter("./answers/t.txt")) {
                            try (PrintWriter out_y = new PrintWriter("./answers/y.txt")) {
                                simpleCallbackSolver.writeVarsToFiles(out_q, out_x, out_t, out_y);
                            }
                        }
                    }
                }
            }

            simpleCallbackSolver.close();

            DrawUtils.newDraw("./answers/", FILENAME, graph);

            //DrawUtils.compareNetClustWithTrueAns("./answers/", "net_clust");

            DrawAPI.run();

            deleteAllFiles("./answers/");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteAllFiles(String path) {
        for (File myFile : Objects.requireNonNull(new File(path).listFiles()))
            if (myFile.isFile()) {
                if (!myFile.delete()) {
                    throw new RuntimeException("not delete files in folder");
                }
            }
    }
}
