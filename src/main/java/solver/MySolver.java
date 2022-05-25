package solver;

import java.io.Closeable;
import java.io.PrintWriter;

public interface MySolver extends Closeable {
    boolean solve() throws Exception;

    void writeVarsToFiles(PrintWriter q, PrintWriter x, PrintWriter t, PrintWriter y) throws Exception;
}
