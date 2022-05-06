package drawing;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class GraphVis {
    public static void main(String[] args) {
        try (InputStream dot = new FileInputStream("./answers/tmp_ans2.dot")) {
            MutableGraph g = new Parser().read(dot);
            Graphviz.fromGraph(g).width(700).render(Format.PNG).toFile(new File("./pictures/graph.png"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
