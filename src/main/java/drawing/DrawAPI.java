package drawing;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import utils.Pair;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DrawAPI extends Application {
    private final static List<Window> windows = new ArrayList<>();

    @Override
    public void start(Stage s) {
        for (Window w : windows) {
            w.run();
        }
    }

    public static void run() {
        launch();
    }

    public static void addWindow(
            String title,
            Map<String, ROC.ROCLine> lines,
            Axis xAxisData,
            Axis yAxisData,
            List<Node> otherObjects
    ) {
        windows.add(new Window(title, lines, xAxisData, yAxisData, otherObjects));
    }

    public record Axis(
            String name,
            boolean auto,
            Double lowerBound,
            Double upperBound,
            Double step
    ) {
        // nothing
    }

    private record Window(
            String title,
            Map<String, ROC.ROCLine> lines,
            Axis xAxisData,
            Axis yAxisData,
            List<Node> otherObjects
    ) {
        public void run() {
            Stage stage = new Stage();
            stage.setTitle("ICA Connected Subgraph");

            NumberAxis xAxis = extractAxis(xAxisData);
            NumberAxis yAxis = extractAxis(yAxisData);

            LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);

            lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: white;");
            lineChart.setTitle(title);
            lineChart.setCreateSymbols(false);
            lineChart.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);

            for (String name : lines.keySet()) {
                if (name.startsWith("nc_")) {
                    continue;
                }
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(name + String.format(" {%.2f} ", lines.get(name).auc_roc()));
                for (Pair<Number, Number> point : lines.get(name).line()) {
                    series.getData().add(new XYChart.Data<>(point.first, point.second));
                }
                lineChart.getData().add(series);
            }

            for (String name : lines.keySet()) {
                if (!name.startsWith("nc_")) {
                    continue;
                }
                assert lines.get(name).line().size() == 3;
                int ind = 1;
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(name + String.format(" {%.2f} ", lines.get(name).auc_roc()));
                double small = 0.004;
                for (Pair<Double, Double> iter : List.of(
                        new Pair<>(-small, small),
                        new Pair<>(small, -small),
                        new Pair<>(small, small),
                        new Pair<>(-small, -small))
                ) {
                    series.getData().add(new XYChart.Data<>(
                            ((double) lines.get(name).line().get(ind).first + iter.first),
                            ((double) lines.get(name).line().get(ind).second + iter.second)
                    ));
                }
                lineChart.getData().add(series);
//                int ind = lines.get(name).threshold_index();
//                XYChart.Series<Number, Number> series = new XYChart.Series<>();
//                series.setName(String.format("cut = %.4f ", lines.get(name).threshold()));
//                double small = 0.002;
//                for (Pair<Double, Double> iter : List.of(
//                        new Pair<>(-small, -small),
//                        new Pair<>(-small, small),
//                        new Pair<>(small, small),
//                        new Pair<>(small, -small))
//                ) {
//                    series.getData().add(new XYChart.Data<>(
//                            ((double) lines.get(name).line().get(ind).first + iter.first),
//                            ((double) lines.get(name).line().get(ind).second + iter.second)
//                    ));
//                }
//                lineChart.getData().add(series);
            }

//            for (int c = 0; c < cnt_color; c++) {
//                lineChart.lookup(".default-color" + c + ".chart-series-line").setStyle("-fx-stroke: " + colors[c] + ";");
//                lineChart.lookup(".default-color" + c + ".chart-line-symbol").setStyle("-fx-background-color: " + colors[c] + ", white;");
//            }

            lineChart.setMinSize(900, 900);
            lineChart.setMaxSize(900, 900);

            Group group = new Group(lineChart);

            if (otherObjects != null) {
                group.getChildren().addAll(otherObjects);
            }

            Scene scene = new Scene(group, 1000, 1000);
            scene.getStylesheets().add("b.css");

            stage.setScene(scene);
            saveToFile(
                    scene,
                    "./pictures/" + title.replaceAll("\\s", "_") + ".png"
            );
            stage.show();
        }

        private static NumberAxis extractAxis(Axis axisData) {
            NumberAxis res = new NumberAxis();
            res.setLabel(axisData.name);
            res.setAutoRanging(axisData.auto);
            if (!axisData.auto) {
                res.setLowerBound(axisData.lowerBound);
                res.setUpperBound(axisData.upperBound);
                res.setTickUnit(axisData.step);
            }
            return res;
        }

        private static void saveToFile(Scene scene, String path) {
            WritableImage image = scene.snapshot(null);
            File file = new File(path);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
