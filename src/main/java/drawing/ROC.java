package drawing;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import utils.Pair;

import java.util.*;

/**
 * static class
 */
public class ROC {
    private static final double EPS = 1e-6;

    public record ROCLine(
            List<Pair<Number, Number>> line,
            Double auc_roc,
            Integer threshold_index,
            Double threshold
    ) {
        // nothing
    }

    public static ROCLine getLine(Double[] predictions, Boolean[] labels) {
        if (predictions.length != labels.length) {
            throw new RuntimeException("invalid data #1 in Roc.draw method");
        }

        int h = 0;
        int w = 0;
        List<Pair<Double, Boolean>> list = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            if (predictions[i] == null || labels[i] == null) {
                throw new RuntimeException("invalid data #2 in Roc.draw method");
            }
            list.add(new Pair<>(predictions[i], labels[i]));
            if (labels[i]) {
                h++;
            } else {
                w++;
            }
        }
        list.sort(Comparator.comparingDouble(x -> x.first));
        Collections.reverse(list);

        double x = 0;
        double y = 0;

        double stepX = 1.0 / (double) w;
        double stepY = 1.0 / (double) h;

        //double max_height = -1;
        int threshold_index = -1;
        double threshold = -1e6;

        List<Pair<Number, Number>> points = new ArrayList<>();
        points.add(new Pair<>(x, y));

        for (int i = 0; i < list.size(); i++) {
            int cnt = i;
            while ((cnt + 1) < list.size() && (Math.abs(list.get(cnt).first - list.get(cnt + 1).first) < EPS)) {
                cnt++;
            }
            int a = 0;
            int b = 0;
            for (int j = i; j <= cnt; j++) {
                if (list.get(j).second) {
                    a++;
                } else {
                    b++;
                }
            }
            y += stepY * a;
            x += stepX * b;
            points.add(new Pair<>(x, y));

            if (x >= 0.01 && threshold_index == -1) {
                threshold_index = points.size() - 1;
                threshold = list.get(threshold_index).first;
            }

            //double curr_height = (y - x) / Math.sqrt(2.0);
            //if (curr_height >= max_height) {
            //    threshold_index = points.size() - 1;
            //    max_height = curr_height;
            //    threshold = list.get(threshold_index).first;
            //}

            i = cnt;
        }

        double numerator = 0;
        double denominator = 0;
        for (Pair<Double, Boolean> el1 : list) {
            for (Pair<Double, Boolean> el2 : list) {
                double b = (!el1.second && el2.second) ? 1 : 0;
                double a;
                if (el1.first > el2.first) {
                    a = 0;
                } else {
                    if (el1.first < el2.first) {
                        a = 1;
                    } else {
                        a = 0.5;
                    }
                }
                numerator += b * a;
                denominator += b;
            }
        }

        if (denominator < EPS) {
            throw new RuntimeException("divide by zero!");
        }

        return new ROCLine(points, numerator / denominator, threshold_index, threshold);
    }

    public static void draw(String title, Map<String, ROCLine> lines) {
        Line diagonal = new Line(65, 665, 685, 40);
        diagonal.setStroke(Color.GRAY);

        DrawAPI.Axis xAxisData = new DrawAPI.Axis(
                "False Positive Rate",
                false,
                0.0,
                1.0,
                0.1
        );

        DrawAPI.Axis yAxisData = new DrawAPI.Axis(
                "True Positive Rate",
                false,
                0.0,
                1.0,
                0.1
        );

        DrawAPI.addWindow(title,
                lines,
                xAxisData,
                yAxisData,
                null
                /*List.of(diagonal)*/
        );
    }
}
