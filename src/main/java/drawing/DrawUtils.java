package drawing;

import graph.Graph;
import utils.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * static class
 */
public class DrawUtils {
    private static final double EPS = 1e-6;

    public static void readResultAndDrawAll(String folder, String title, Graph graph) {
        try {
            Double[] q = readAsDoubleArray(folder + "q.txt");
            Double[] x = readAsDoubleArray(folder + "x.txt");
            Boolean[] p0 = readAsBooleanArray(folder + "p_ans_0.txt");
            Boolean[] p1 = readAsBooleanArray(folder + "p_ans_1.txt");
            Boolean[] p2 = readAsBooleanArray(folder + "p_ans_2.txt");
            Boolean[] p3 = readAsBooleanArray(folder + "p_ans_3.txt");

            Map<String, ROC.ROCLine> lines = new TreeMap<>();

            lines.put("ans_0", ROC.getLine(q, p0));
            lines.put("ans_1", ROC.getLine(q, p1));
            lines.put("ans_2", ROC.getLine(q, p2));
            lines.put("ans_3", ROC.getLine(q, p3));

            List<Pair<Double, Boolean[]>> modules = new ArrayList<>();
            modules.add(new Pair<>(lines.get("ans_0").threshold(), p0));
            modules.add(new Pair<>(lines.get("ans_1").threshold(), p1));
            modules.add(new Pair<>(lines.get("ans_2").threshold(), p2));
            modules.add(new Pair<>(lines.get("ans_3").threshold(), p3));

            graph.saveAsDOT("./answers/", title, x, q, modules);

            ROC.draw(title, lines);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Boolean[] readAsBooleanArray(String f) throws IOException {
        BufferedReader arg = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8));
        return arg.lines().map(Double::parseDouble).map(x -> (Math.abs(x - 1.0) < EPS)).toList().toArray(new Boolean[0]);
    }

    private static Double[] readAsDoubleArray(String f) throws IOException {
        BufferedReader arg = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8));
        return arg.lines().map(Double::parseDouble).toList().toArray(new Double[0]);
    }
}
