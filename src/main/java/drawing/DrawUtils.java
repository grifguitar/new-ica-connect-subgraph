package drawing;

import graph.Graph;
import utils.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * static class
 */
public class DrawUtils {
    private static final double EPS = 1e-6;
    private static final int ANS_FILES_COUNT = 2;

    public static void newDraw(String folder, String title, Graph graph) {
        try (PrintWriter agg1 = new PrintWriter(new FileOutputStream("./aggregate/agg1.txt", true))) {
            try (PrintWriter agg2 = new PrintWriter(new FileOutputStream("./aggregate/agg2.txt", true))) {
                agg1.println("--------------------");
                agg2.println("--------------------");

                Double[] clust_size = readAsDoubleArray(folder + "0_clust_size.txt");
                Double[] module_size = readAsDoubleArray(folder + "0_module_size.txt");
                Double[] q = readAsDoubleArray(folder + "q.txt");
                Double[] x = readAsDoubleArray(folder + "x.txt");
                Double[] t = readAsDoubleArray(folder + "t.txt");
                Double[] y = readAsDoubleArray(folder + "y.txt");

//                Double[] ica_f = readAsDoubleArray(folder + "ica_f.txt");
//                Double[] ica_g = readAsDoubleArray(folder + "ica_g.txt");

                List<Double[]> clusters = new ArrayList<>();
                for (int base_cnt = 0; base_cnt < ANS_FILES_COUNT; base_cnt++) {
                    for (int clustNum = 0; clustNum < clust_size[base_cnt]; clustNum++) {
                        Double[] n = readAsDoubleArray(folder + (base_cnt + 1) + "_nc_ans_" + clustNum + ".txt");
                        clusters.add(n);
                    }
                }

                int bestModule = -1;
                double bestValue = -1;
                Map<Integer, List<String>> results = new HashMap<>();
                for (int modNum = 0; modNum < module_size[0]; modNum++) {
                    Boolean[] p = readAsBooleanArray(folder + "p_ans_" + modNum + ".txt");

                    results.put(modNum, new ArrayList<>());

                    //Map<String, ROC.ROCLine> lines = new TreeMap<>();
                    Map<String, ROC.ROCLine> myLine = new TreeMap<>();
                    Map<String, ROC.ROCLine> bestNetClustLine = new TreeMap<>();
                    Map<String, ROC.ROCLine> otherNetClustLine = new TreeMap<>();

                    int clusters_ind = 0;
                    List<Double> best_f1score_clust = new ArrayList<>();
                    List<Integer> best_tpfp_clust = new ArrayList<>();
                    List<Pair<String, ROC.ROCLine>> best_lines_clust = new ArrayList<>();

                    for (int base_cnt = 0; base_cnt < ANS_FILES_COUNT; base_cnt++) {
                        String base;
                        if (base_cnt == 0) {
                            base = "0.25";
                        } else if (base_cnt == 1) {
                            base = "0.4";
                        } else if (base_cnt == 2) {
                            base = "0.5";
                        } else {
                            throw new RuntimeException("unexpected");
                        }

                        best_f1score_clust.add(-1.0);
                        best_tpfp_clust.add(-1);
                        best_lines_clust.add(null);

                        for (int clustNum = 0; clustNum < clust_size[base_cnt]; clustNum++) {
                            Double[] n = clusters.get(clusters_ind++);
                            ROC.ROCLine line = ROC.getLine(n, p, null);
                            double[] metrics = calcMetrics(n, p, 1 - EPS);
                            if (metrics[2] > best_f1score_clust.get(base_cnt)) {
                                best_f1score_clust.set(base_cnt, metrics[2]);
                                best_tpfp_clust.set(base_cnt, (int) Math.round(metrics[3]));
                                best_lines_clust.set(base_cnt, new Pair<>("NC_" + base + "_" + clustNum, line));
                            }
                            agg1.println(title + "_module_" + modNum + "_nc_" + clustNum + "_" + base + ", metrics = " + Arrays.toString(metrics));
                            otherNetClustLine.put("NC_" + base + "_" + clustNum, line);
                        }
                    }

                    ROC.ROCLine line_x = ROC.getLine(q, p, best_tpfp_clust);
                    ROC.ROCLine line_y = ROC.getLine(t, p, best_tpfp_clust);

                    for (int base_cnt = 0; base_cnt < ANS_FILES_COUNT; base_cnt++) {
                        String base;
                        if (base_cnt == 0) {
                            base = "0.25";
                        } else if (base_cnt == 1) {
                            base = "0.4";
                        } else if (base_cnt == 2) {
                            base = "0.5";
                        } else {
                            throw new RuntimeException("unexpected");
                        }

                        String str_nc = title + "_module_" + modNum + "_nc_" + base + ", best_f1score = " + best_f1score_clust.get(base_cnt);
                        agg2.println(str_nc);
                        results.get(modNum).add(str_nc);
                        bestNetClustLine.put(best_lines_clust.get(base_cnt).first, best_lines_clust.get(base_cnt).second);
                        otherNetClustLine.remove(best_lines_clust.get(base_cnt).first);

                        double[] m_x = calcMetrics(q, p, line_x.threshold().get(base_cnt));
                        double[] m_y = calcMetrics(t, p, line_y.threshold().get(base_cnt));
                        if (m_x[2] > m_y[2]) {
                            agg1.println(title + "_module_" + modNum + "_x_" + base + ", metrics = " + Arrays.toString(m_x));
                            String str_my = title + "_module_" + modNum + "_x_" + base + ", f1score = " + m_x[2];
                            agg2.println(str_my);
                            results.get(modNum).add(str_my);
                            if (m_x[2] > bestValue) {
                                bestValue = m_x[2];
                                bestModule = modNum;
                            }
                        } else {
                            agg1.println(title + "_module_" + modNum + "_y_" + base + ", metrics = " + Arrays.toString(m_y));
                            String str_my = title + "_module_" + modNum + "_y_" + base + ", f1score = " + m_y[2];
                            agg2.println(str_my);
                            results.get(modNum).add(str_my);
                            if (m_y[2] > bestValue) {
                                bestValue = m_y[2];
                                bestModule = modNum;
                            }
                        }
                    }

                    myLine.put("POSITIVE", line_x);
                    myLine.put("NEGATIVE", line_y);

//                    lines.put("z1", ROC.getLine(ica_f, p));
//                    lines.put("z2", ROC.getLine(ica_g, p));

                    graph.saveAsDOT(
                            "./pictures/",
                            title + "_x",
                            x,
                            q,
                            new Pair<>(line_x.threshold().get(line_x.threshold().size() - 1), p),
                            modNum,
                            false
                    );
                    graph.saveAsDOT(
                            "./pictures/",
                            title + "_y",
                            y,
                            t,
                            new Pair<>(line_y.threshold().get(line_y.threshold().size() - 1), p),
                            modNum,
                            false
                    );

                    ROC.draw(title + "_module_" + modNum, myLine, bestNetClustLine, otherNetClustLine);
                }

                try (PrintWriter agg3 = new PrintWriter(new FileOutputStream("./aggregate/agg3.txt", true))) {
                    agg3.println("--------------------");
                    for (String str : results.get(bestModule)) {
                        agg3.println(str);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static double[] calcMetrics(Double[] predictions, Boolean[] labels, double threshold) {
        double TP = 0, TN = 0, FP = 0, FN = 0, precision = 0, recall = 0, f1score = 0;
        for (int i = 0; i < labels.length; i++) {
            if (predictions[i] >= threshold && labels[i]) TP += 1;
            if (predictions[i] >= threshold && !labels[i]) FP += 1;
            if (predictions[i] < threshold && labels[i]) FN += 1;
            if (predictions[i] < threshold && !labels[i]) TN += 1;
        }
        if (TP + FP != 0) {
            precision = TP / (TP + FP);
        }
        if (TP + FN != 0) {
            recall = TP / (TP + FN);
        }
        if (precision + recall != 0) {
            f1score = 2 * precision * recall / (precision + recall);
        }
        return new double[]{precision, recall, f1score, TP + FP};
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
