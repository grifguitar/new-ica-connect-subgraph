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

    public static void compareNetClustWithTrueAns(String folder, String title) {
        try {
            Boolean[] p0 = readAsBooleanArray(folder + "p_ans_0.txt");
            Boolean[] p1 = readAsBooleanArray(folder + "p_ans_1.txt");
            Boolean[] p2 = readAsBooleanArray(folder + "p_ans_2.txt");
            Boolean[] p3 = readAsBooleanArray(folder + "p_ans_3.txt");

            Double[] n0 = readAsDoubleArray(folder + "net_cl_ans_0.txt");
            Double[] n1 = readAsDoubleArray(folder + "net_cl_ans_1.txt");
            Double[] n2 = readAsDoubleArray(folder + "net_cl_ans_2.txt");
            Double[] n3 = readAsDoubleArray(folder + "net_cl_ans_3.txt");
            Double[] n4 = readAsDoubleArray(folder + "net_cl_ans_4.txt");
            List<Double[]> nn = List.of(n0, n1, n2, n3, n4);

            for (int i = 0; i < nn.size(); i++) {
                Map<String, ROC.ROCLine> lines = new TreeMap<>();

                lines.put("nc" + i + "_ans_0", ROC.getLine(nn.get(i), p0));
                lines.put("nc" + i + "_ans_1", ROC.getLine(nn.get(i), p1));
                lines.put("nc" + i + "_ans_2", ROC.getLine(nn.get(i), p2));
                lines.put("nc" + i + "_ans_3", ROC.getLine(nn.get(i), p3));

                ROC.draw(title + i, lines);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void newDraw(String folder, String title, Graph graph) {
        try {
            Double[] clust_size = readAsDoubleArray(folder + "0_clust_size.txt");
            Double[] module_size = readAsDoubleArray(folder + "0_module_size.txt");
            Double[] q = readAsDoubleArray(folder + "q.txt");
            Double[] x = readAsDoubleArray(folder + "x.txt");
            Double[] t = readAsDoubleArray(folder + "t.txt");
            Double[] y = readAsDoubleArray(folder + "y.txt");

            List<Double[]> clusters = new ArrayList<>();
            for (int cnt = 1; cnt <= 3; cnt++) {
                for (int clustNum = 0; clustNum < clust_size[cnt - 1]; clustNum++) {
                    Double[] n = readAsDoubleArray(folder + cnt + "_nc_ans_" + clustNum + ".txt");
                    clusters.add(n);
                }
            }

            for (int modNum = 0; modNum < module_size[0]; modNum++) {
                Boolean[] p = readAsBooleanArray(folder + "p_ans_" + modNum + ".txt");

                Map<String, ROC.ROCLine> lines = new TreeMap<>();

                int ind_0 = 0;
                for (int cnt = 1; cnt <= 3; cnt++) {
                    String base;
                    if (cnt == 1) {
                        base = "0.25";
                    } else if (cnt == 2) {
                        base = "0.4";
                    } else {
                        base = "0.5";
                    }
                    for (int clustNum = 0; clustNum < clust_size[cnt - 1]; clustNum++) {
                        Double[] n = clusters.get(ind_0++);
                        lines.put("nc_" + clustNum + ":" + base, ROC.getLine(n, p));
                    }
                }

                lines.put("x", ROC.getLine(q, p));
                lines.put("y", ROC.getLine(t, p));

                ROC.draw(title + "_module_" + modNum, lines);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void readResultAndDrawAll(String folder, String title, Graph graph) {
        try {
            Double[] q = readAsDoubleArray(folder + "q.txt");
            Double[] x = readAsDoubleArray(folder + "x.txt");
            Double[] t = readAsDoubleArray(folder + "t.txt");
            Double[] y = readAsDoubleArray(folder + "y.txt");
            Boolean[] p0 = readAsBooleanArray(folder + "p_ans_0.txt");
            Boolean[] p1 = readAsBooleanArray(folder + "p_ans_1.txt");
            Boolean[] p2 = readAsBooleanArray(folder + "p_ans_2.txt");
            Boolean[] p3 = readAsBooleanArray(folder + "p_ans_3.txt");

            Map<String, ROC.ROCLine> lines_q = new TreeMap<>();

            lines_q.put("ans_0", ROC.getLine(q, p0));
            lines_q.put("ans_1", ROC.getLine(q, p1));
            lines_q.put("ans_2", ROC.getLine(q, p2));
            lines_q.put("ans_3", ROC.getLine(q, p3));

            List<Pair<Double, Boolean[]>> modules_q = new ArrayList<>();
            modules_q.add(new Pair<>(lines_q.get("ans_0").threshold(), p0));
            modules_q.add(new Pair<>(lines_q.get("ans_1").threshold(), p1));
            modules_q.add(new Pair<>(lines_q.get("ans_2").threshold(), p2));
            modules_q.add(new Pair<>(lines_q.get("ans_3").threshold(), p3));

            for (int i = 0; i < 4; i++) {
                graph.saveAsDOT("./answers/", title + "x", x, q, modules_q, new Pair<>(i, null));
            }

            ROC.draw(title + "x", lines_q);

            Map<String, ROC.ROCLine> lines_t = new TreeMap<>();

            lines_t.put("ans_0", ROC.getLine(t, p0));
            lines_t.put("ans_1", ROC.getLine(t, p1));
            lines_t.put("ans_2", ROC.getLine(t, p2));
            lines_t.put("ans_3", ROC.getLine(t, p3));

            List<Pair<Double, Boolean[]>> modules_t = new ArrayList<>();
            modules_t.add(new Pair<>(lines_t.get("ans_0").threshold(), p0));
            modules_t.add(new Pair<>(lines_t.get("ans_1").threshold(), p1));
            modules_t.add(new Pair<>(lines_t.get("ans_2").threshold(), p2));
            modules_t.add(new Pair<>(lines_t.get("ans_3").threshold(), p3));

            for (int i = 0; i < 4; i++) {
                graph.saveAsDOT("./answers/", title + "y", y, t, modules_t, new Pair<>(i, null));
            }

            ROC.draw(title + "y", lines_t);

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
