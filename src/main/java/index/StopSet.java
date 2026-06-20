package index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StopSet {

    public static Set<String> getStopSetFromFile() throws IOException {
        Path stopWordsPath = Paths.get("src", "stop_words", "stop_words_moderate.txt");
        List<String> stopWords = Files.readAllLines(stopWordsPath);
        return Collections.unmodifiableSet(stopWords.stream().collect(Collectors.toSet()));
    }
}
