package index

import groovy.transform.CompileStatic
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.search.similarities.Similarity
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic
enum IndexEnum {

    CRISIS3('indexes/Crisis3', 'datasets/Crisis3', 3),
    CRISIS4('indexes/crisis4', 'datasets/crisis4', 4),
    CRISIS6('indexes/CrisisLexT6', 'datasets/CrisisLexT6', 6),

     NG3('indexes/NG3', 'datasets/NG3', 3),
     NG5('indexes/NG5', 'datasets/NG5', 5),
     NG6('indexes/NG6', 'datasets/NG6', 6),

     R4('indexes/R4', 'datasets/R4', 4),
     R5('indexes/R5', 'datasets/R5', 5),
     R6('indexes/R6', 'datasets/R6', 6)

    String indexPath
    String docsPath
    int numberOfClasses

    IndexEnum(String indexPath, String docsPath, int numberOfClasses) {
        this.numberOfClasses = numberOfClasses
        this.indexPath = indexPath
        this.docsPath = docsPath
    }

    String toString() {
        return "${this.name()} indexPath: $indexPath docsPath: $docsPath numberOfClasses: $numberOfClasses  "
    }

    IndexReader getIndexReader() {
        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        IndexReader ir = DirectoryReader.open(directory)
        return ir
    }

    IndexSearcher getIndexSearcher() {
        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        IndexReader ir = DirectoryReader.open(directory)
        IndexSearcher is = new IndexSearcher(ir)

       // is.setSimilarity(Indexes.similarity)
        return is
    }
}

@CompileStatic
class Indexes {

    //current index
    static IndexEnum index
    static IndexSearcher indexSearcher
    static IndexReader indexReader
    public static List<TermQuery> termQueryList

    //static Map<TermQuery, List<Tuple2<TermQuery, Double>>> termQueryIntersectMap
    public static Map<String, List<TermQuery>> orderedIntersectMap

    // Lucene field names
    static final String FIELD_CATEGORY_NAME = 'category',
                        FIELD_CONTENTS = 'contents',
                        FIELD_PATH = 'path',
                        FIELD_TEST_TRAIN = 'test_train',
                        FIELD_CATEGORY_NUMBER = 'categoryNumber',
                        FIELD_QUERY_ASSIGNED_CLUSTER = 'assignedClass',
                        FIELD_DOCUMENT_ID = 'document_id'


   // static CharArraySet stopCharSet = new CharArraySet(stopSet, true);
   // static final Analyzer analyzer = new StandardAnalyzer(stopCharSet)
    // public static final Similarity similarity = //new BM25Similarity()    //new ClassicSimilarity()
    //static final Analyzer analyzer = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET)  //new StandardAnalyzer()  //new EnglishAnalyzer();
    static final Analyzer analyzer = new HashtagPreservingEnglishAnalyzer()

    static void setIndex(IndexEnum indexEnum) {
        index = indexEnum
        indexSearcher = index.getIndexSearcher()
        indexReader = indexSearcher.getIndexReader()
    }

    static void setImportantTermQueryList(int maxSize = ImportantTermQueries.MAX_TERMQUERYLIST_SIZE) {
        termQueryList = ImportantTermQueries.getTFIDFTermQueryList(indexReader, maxSize) asImmutable()
        MapWordToIntersectingTermQueryList mapWordToIntersectingTermQueryList = new MapWordToIntersectingTermQueryList()
        orderedIntersectMap = mapWordToIntersectingTermQueryList.getIntersectingTerms(termQueryList)
    }
}
