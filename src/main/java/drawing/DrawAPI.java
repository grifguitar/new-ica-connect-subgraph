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

    public record Axis(
            String name,
            boolean auto,
            Double lowerBound,
            Double upperBound,
            Double step
    ) {
        // nothing
    }

    public static void addWindow(
            String title,
            Axis xAxisData,
            Axis yAxisData,
            Map<String, ROC.ROCLine> myLine,
            Map<String, ROC.ROCLine> bestNetClustLine,
            Map<String, ROC.ROCLine> otherNetClustLine,
            List<Node> otherObjects
    ) {
        windows.add(
                new Window(title,
                        xAxisData,
                        yAxisData,
                        myLine,
                        bestNetClustLine,
                        otherNetClustLine,
                        otherObjects
                )
        );
    }

    private record Window(
            String title,
            Axis xAxisData,
            Axis yAxisData,
            Map<String, ROC.ROCLine> myLine,
            Map<String, ROC.ROCLine> bestNetClustLine,
            Map<String, ROC.ROCLine> otherNetClustLine,
            List<Node> otherObjects
    ) {
        public void run() {
            Stage stage = new Stage();
            stage.setTitle("ICA Connected Subgraph");

            NumberAxis xAxis = extractAxis(xAxisData);
            NumberAxis yAxis = extractAxis(yAxisData);

            LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);

            lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: white;");
            lineChart.setTitle("AUC ROC for \"" + title + "\"");
            lineChart.setCreateSymbols(false);
            lineChart.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);

            for (String lineName : myLine.keySet()) {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(lineName + String.format(" (%.2f) ", myLine.get(lineName).auc_roc()));

                for (Pair<Number, Number> point : myLine.get(lineName).line()) {
                    series.getData().add(new XYChart.Data<>(point.first, point.second));
                }

                lineChart.getData().add(series);
            }

            for (String lineName : bestNetClustLine.keySet()) {
                assert bestNetClustLine.get(lineName).line().size() == 3;
                XYChart.Series<Number, Number> series = getMarker(
                        bestNetClustLine.get(lineName).line().get(1),
                        0.005
                );

                series.setName(lineName + String.format(" (%.2f) ", bestNetClustLine.get(lineName).auc_roc()));
                lineChart.getData().add(series);
            }

            for (String lineName : otherNetClustLine.keySet()) {
                assert otherNetClustLine.get(lineName).line().size() == 3;
                XYChart.Series<Number, Number> series = getMarker(
                        otherNetClustLine.get(lineName).line().get(1),
                        0.003
                );

                series.setName(" ");
                lineChart.getData().add(series);
            }

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

        private static XYChart.Series<Number, Number> getMarker(Pair<Number, Number> point, double small) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();

            for (Pair<Double, Double> iter : List.of(
                    new Pair<>(-small, small),
                    new Pair<>(small, -small),
                    new Pair<>(small, small),
                    new Pair<>(-small, -small))
            ) {
                series.getData().add(new XYChart.Data<>(
                        ((double) point.first + iter.first),
                        ((double) point.second + iter.second)
                ));
            }

            return series;
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
