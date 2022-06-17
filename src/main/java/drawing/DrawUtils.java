package drawing;

import graph.Graph;
import utils.Pair;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * static class
 */
public class DrawUtils {
    private static final double EPS = 1e-6;
    public static final int ANS_FILES_COUNT = 3;

    public static void newDraw(String folder, String title, Graph graph) {
        try (PrintWriter agg1 = new PrintWriter(new FileOutputStream("./aggregate/agg1.txt", true))) {
            try (PrintWriter agg2 = new PrintWriter(new FileOutputStream("./aggregate/agg2.txt", true))) {
                agg1.println("--------------------");
                agg2.println("--------------------");

                Double[] clust_size = readAsDoubleArray(folder + "0_clust_size.txt");
                Double[] module_size = readAsDoubleArray(folder + "0_module_size.txt");
                Double[] fast_ica_size = readAsDoubleArray(folder + "0_fast_ica_size.txt");

                Double[] q = readAsDoubleArray(folder + "q.txt");
                Double[] x = readAsDoubleArray(folder + "x.txt");
                Double[] t = readAsDoubleArray(folder + "t.txt");
                Double[] y = readAsDoubleArray(folder + "y.txt");

                List<Double[]> fast_ica = new ArrayList<>();
                for (int i = 0; i < fast_ica_size[0]; i++) {
                    Double[] ica = readAsDoubleArray(folder + "ica_ans_" + i + ".txt");
                    Double[] ica_f = new Double[ica.length];
                    Double[] ica_g = new Double[ica.length];
                    for (int j = 0; j < ica.length; j++) {
                        if (ica[j] >= 0) {
                            ica_f[j] = Math.abs(ica[j]);
                            ica_g[j] = 0.0;
                        } else {
                            ica_f[j] = 0.0;
                            ica_g[j] = Math.abs(ica[j]);
                        }
                    }
                    fast_ica.add(ica_f);
                    fast_ica.add(ica_g);
                }

                List<Double[]> clusters = new ArrayList<>();
                for (int base_cnt = 0; base_cnt < ANS_FILES_COUNT; base_cnt++) {
                    for (int clustNum = 0; clustNum < clust_size[base_cnt]; clustNum++) {
                        Double[] n = readAsDoubleArray(folder + (base_cnt + 1) + "_nc_ans_" + clustNum + ".txt");
                        clusters.add(n);
                    }
                }

                List<Pair<Double, Integer>> theBest = new ArrayList<>();

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
                                double val = BigDecimal.valueOf(metrics[2]).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
                                best_f1score_clust.set(base_cnt, val);
                                best_tpfp_clust.set(base_cnt, (int) Math.round(metrics[3]));
                                best_lines_clust.set(base_cnt, new Pair<>("NC_" + base + "_" + clustNum, line));
                            }
                            agg1.println(title + "_module_" + modNum + "_nc_" + clustNum + "_" + base + ", metrics = " + Arrays.toString(metrics));
                            otherNetClustLine.put("NC_" + base + "_" + clustNum, line);
                        }
                    }

                    ROC.ROCLine line_x = ROC.getLine(q, p, best_tpfp_clust);
                    ROC.ROCLine line_y = ROC.getLine(t, p, best_tpfp_clust);

                    myLine.put("POSITIVE", line_x);
                    myLine.put("NEGATIVE", line_y);

                    double max_ica_auc_roc = 0;
                    for (int i = 0; i < fast_ica.size(); i++) {
                        ROC.ROCLine ica_line = ROC.getLine(fast_ica.get(i), p, null);
                        myLine.put("ICA" + i, ica_line);
                        if (max_ica_auc_roc < ica_line.auc_roc()) {
                            max_ica_auc_roc = ica_line.auc_roc();
                        }
                    }
                    max_ica_auc_roc = BigDecimal.valueOf(max_ica_auc_roc).setScale(4, RoundingMode.HALF_UP).doubleValue();

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

                        String str_nc = title + "_module_" + modNum + "_nc_" + base + "," + best_f1score_clust.get(base_cnt);
                        agg2.println(str_nc);
                        results.get(modNum).add(str_nc);
                        bestNetClustLine.put(best_lines_clust.get(base_cnt).first, best_lines_clust.get(base_cnt).second);
                        otherNetClustLine.remove(best_lines_clust.get(base_cnt).first);

                        double[] m_x = calcMetrics(q, p, line_x.threshold().get(base_cnt));
                        double[] m_y = calcMetrics(t, p, line_y.threshold().get(base_cnt));
                        double val_x = BigDecimal.valueOf(m_x[2]).setScale(4, RoundingMode.HALF_UP).doubleValue();
                        double val_y = BigDecimal.valueOf(m_y[2]).setScale(4, RoundingMode.HALF_UP).doubleValue();
                        double auc_x = BigDecimal.valueOf(line_x.auc_roc()).setScale(4, RoundingMode.HALF_UP).doubleValue();
                        double auc_y = BigDecimal.valueOf(line_y.auc_roc()).setScale(4, RoundingMode.HALF_UP).doubleValue();
                        if (val_x > val_y) {
                            agg1.println(title + "_module_" + modNum + "_x_" + base + ", metrics = " + Arrays.toString(m_x));
                            String str_my = title + "_module_" + modNum + "_x_" + base + "," + val_x + "," + auc_x + "," + max_ica_auc_roc;
                            agg2.println(str_my);
                            results.get(modNum).add(str_my);
                            theBest.add(new Pair<>(m_x[2], modNum));
                        } else {
                            agg1.println(title + "_module_" + modNum + "_y_" + base + ", metrics = " + Arrays.toString(m_y));
                            String str_my = title + "_module_" + modNum + "_y_" + base + "," + val_y + "," + auc_y + "," + max_ica_auc_roc;
                            agg2.println(str_my);
                            results.get(modNum).add(str_my);
                            theBest.add(new Pair<>(m_y[2], modNum));
                        }
                    }
                    if (ANS_FILES_COUNT == 0) {
                        double[] m_x = calcMetrics(q, p, line_x.threshold().get(line_x.threshold().size() - 1));
                        double[] m_y = calcMetrics(t, p, line_y.threshold().get(line_y.threshold().size() - 1));
                        if (m_x[2] > m_y[2]) {
                            agg1.println(title + "_module_" + modNum + "_x_" + "nobase" + ", metrics = " + Arrays.toString(m_x));
                            String str_my = title + "_module_" + modNum + "_x_" + "nobase" + "," + m_x[2] + "," + line_x.auc_roc() + "," + max_ica_auc_roc;
                            agg2.println(str_my);
                            results.get(modNum).add(str_my);
                            theBest.add(new Pair<>(m_x[2], modNum));
                        } else {
                            agg1.println(title + "_module_" + modNum + "_y_" + "nobase" + ", metrics = " + Arrays.toString(m_y));
                            String str_my = title + "_module_" + modNum + "_y_" + "nobase" + "," + m_y[2] + "," + line_y.auc_roc() + "," + max_ica_auc_roc;
                            agg2.println(str_my);
                            results.get(modNum).add(str_my);
                            theBest.add(new Pair<>(m_y[2], modNum));
                        }
                    }

//                    graph.saveAsDOT(
//                            "./pictures/",
//                            title + "_x",
//                            x,
//                            q,
//                            new Pair<>(line_x.threshold().get(line_x.threshold().size() - 1), p),
//                            modNum,
//                            false
//                    );
//                    graph.saveAsDOT(
//                            "./pictures/",
//                            title + "_y",
//                            y,
//                            t,
//                            new Pair<>(line_y.threshold().get(line_y.threshold().size() - 1), p),
//                            modNum,
//                            false
//                    );

                    ROC.draw(title + "_module_" + modNum, myLine, bestNetClustLine, otherNetClustLine);
                }

                try (PrintWriter agg3 = new PrintWriter(new FileOutputStream("./aggregate/agg3.txt", true))) {
                    agg3.println("--------------------");
                    theBest.sort(Comparator.comparing(p -> p.first));
                    Collections.reverse(theBest);
                    int countBest = 0;
                    HashSet<Integer> st = new HashSet<>();
                    for (Pair<Double, Integer> pair : theBest) {
                        if (!st.contains(pair.second)) {
                            st.add(pair.second);
                            for (String str : results.get(pair.second)) {
                                agg3.println(str);
                            }
                            countBest++;
                        }
                        if (countBest >= 2) break;
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
