package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.pattern.PatternTokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

public class HashtagPreservingAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Pattern pattern = Pattern.compile("[a-zA-Z0-9#]+");
        Tokenizer source = new PatternTokenizer(pattern, 0);

        TokenStream filter = new LowerCaseFilter(source);

        try {
            Path stopWordsPath = Paths.get("src", "stop_words", "stop_words_moderate.txt");            
       //     Path stopWordsPath = Paths.get("src", "stop_words", "nltkStopWords.txt");

            List<String> stopSet = Files.readAllLines(stopWordsPath);
            CharArraySet stopCharSet = new CharArraySet(stopSet, true);
            filter = new StopFilter(filter, stopCharSet);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load stop words file", e);
        }

        return new TokenStreamComponents(source, filter);
    }
}
