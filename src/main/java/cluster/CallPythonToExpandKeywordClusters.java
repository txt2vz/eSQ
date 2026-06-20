package cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class CallPythonToExpandKeywordClusters {

    static final String PYTHON_LOCATION = "C:\\Users\\lauri\\eSQ\\eSQ\\.venv\\Scripts\\python.exe";

    public static int processPythonExpandClusters() throws IOException, InterruptedException {
        System.out.println("Calling Python script to expand keyword clusters...");

        ProcessBuilder processBuilder = new ProcessBuilder(PYTHON_LOCATION, "src\\main\\python\\expandKeywordClusters.py");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        System.out.println("********************************************************   ");
        System.out.println("Output from Python script:\n" + output);
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.println("Python finished successfully");
        } else {
            System.out.println("Python failed with exit code: " + exitCode);
        }
        return exitCode;
    }
}
