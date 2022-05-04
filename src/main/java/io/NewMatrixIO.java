package io;

import utils.Matrix;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NewMatrixIO {
    public static Matrix read(
            String f, boolean withName,
            Map<String, Integer> namingMap,
            Map<Integer, String> revNamingMap
    ) throws IOException {
        Scanner scanner = new Scanner(new FileReader(f, StandardCharsets.UTF_8));

        int D = -1;
        int lineNum = 0;

        List<List<Double>> matrix = new ArrayList<>();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] tokens = line.split("\\s");

            if (D == -1) {
                D = tokens.length;
            } else if (tokens.length != D) {
                throw new RuntimeException("expected " + D + " tokens in " + lineNum + " row");
            }

            List<Double> row = new ArrayList<>();
            for (int i = 0; i < tokens.length; i++) {
                if (i == 0 && withName) {
                    String name = tokens[0];
                    if (namingMap.containsKey(name)) {
                        throw new RuntimeException("expected unique name for any line, found: " + name);
                    }
                    namingMap.put(name, lineNum);
                    if (revNamingMap.containsKey(lineNum)) {
                        throw new RuntimeException("expected unique int for any line, found: " + lineNum);
                    }
                    revNamingMap.put(lineNum, name);
                    continue;
                }
                row.add(Double.parseDouble(tokens[i]));
            }

            matrix.add(row);
            lineNum++;
        }

        return new Matrix(matrix);
    }
}
