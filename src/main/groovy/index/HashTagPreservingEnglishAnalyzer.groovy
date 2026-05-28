import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet
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
       
        // This regex matches sequences of alphanumeric characters and '#'
        Pattern pattern = Pattern.compile("[a-zA-Z0-9#]+");
        Tokenizer source = new PatternTokenizer(pattern, 0);
       
        TokenStream filter = new LowerCaseFilter(source);

        List<String> stopSet = new File("src//stop_words//stop_words_moderate.txt") as List<String>
        CharArraySet stopCharSet = new CharArraySet(stopSet, true);
  
        filter = new StopFilter(filter, stopCharSet);
       // filter = new StopFilter(filter, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);

        return new TokenStreamComponents(source, filter);
    }
}