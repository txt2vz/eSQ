import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import java.util.regex.Pattern;

public class HashtagPreservingEnglishAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        // 1. Tokenizer: Match words, numbers, and the '#' symbol.
        // This regex matches sequences of alphanumeric characters and '#'
        Pattern pattern = Pattern.compile("[a-zA-Z0-9#]+");
        Tokenizer source = new PatternTokenizer(pattern, 0);

        // 2. Filter: Convert text to lowercase
        TokenStream filter = new LowerCaseFilter(source);

        // 3. Filter: Remove standard English stop words (e.g., "the", "is", "and")
        filter = new StopFilter(filter, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);

        return new TokenStreamComponents(source, filter);
    }
}