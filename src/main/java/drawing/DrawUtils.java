package drawing;

import utils.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * static class
 */
public class DrawUtils {
    public static void drawingAnswer(String folder, String title) {
        try {
            Double[] p = extractDouble(new BufferedReader(new FileReader(folder + "p.txt", StandardCharsets.UTF_8)));
            Boolean[] p1 = extract(new BufferedReader(new FileReader(folder + "p_ans_0.txt", StandardCharsets.UTF_8)));
            Boolean[] p2 = extract(new BufferedReader(new FileReader(folder + "p_ans_1.txt", StandardCharsets.UTF_8)));
            Boolean[] p3 = extract(new BufferedReader(new FileReader(folder + "p_ans_2.txt", StandardCharsets.UTF_8)));
            Boolean[] p4 = extract(new BufferedReader(new FileReader(folder + "p_ans_3.txt", StandardCharsets.UTF_8)));

            Map<String, Pair<List<Pair<Number, Number>>, String>> lines = new TreeMap<>();

            lines.put("p-vs-0", ROC.getLine(p, p1));
            lines.put("p-vs-1", ROC.getLine(p, p2));
            lines.put("p-vs-2", ROC.getLine(p, p3));
            lines.put("p-vs-3", ROC.getLine(p, p4));

            ROC.draw(title, lines);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Boolean[] extract(BufferedReader arg) {
        return arg.lines().map(Double::parseDouble).map(x -> (x.intValue() == 1)).toList().toArray(new Boolean[0]);
    }

    private static Double[] extractDouble(BufferedReader arg) {
        return arg.lines().map(Double::parseDouble).toList().toArray(new Double[0]);
    }
}
