package index

import groovy.transform.CompileStatic
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic
enum IndexEnum {

    CRISIS3('indexes/Crisis3', 3),
    CRISIS4('indexes/crisis4', 4),

    NG3('indexes/NG3', 3),
    NG4b('indexes/NG4b', 4),
    NG5('indexes/NG5', 5),
    NG6('indexes/NG6', 6),

    R4('indexes/R4', 4),
    R5('indexes/R5', 5),
    R6('indexes/R6', 6),

    space('indexes/space', 1)


    // private final Similarity similarity = new BM25Similarity()  // new ClassicSimilarity()
    String pathString
    int numberOfClasses

    IndexEnum(String pathString, int numberOfClasses) {
        this.numberOfClasses = numberOfClasses
        this.pathString = pathString
    }

    String toString() {
        return "${this.name()} path: $pathString numberOfClasses: $numberOfClasses  "
    }

    IndexReader getIndexReader() {
        Path path = Paths.get(pathString)
        Directory directory = FSDirectory.open(path)
        IndexReader ir = DirectoryReader.open(directory)
        return ir
    }

    IndexSearcher getIndexSearcher() {
        Path path = Paths.get(pathString)
        Directory directory = FSDirectory.open(path)
        IndexReader ir = DirectoryReader.open(directory)
        IndexSearcher is = new IndexSearcher(ir)

        //    is.setSimilarity(similarity)
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
    static Map<TermQuery, List<Tuple2<TermQuery, Double>>> termQueryIntersectMap

    // Lucene field names
    static final String FIELD_CATEGORY_NAME = 'category',
                        FIELD_CONTENTS = 'contents',
                        FIELD_PATH = 'path',
                        FIELD_TEST_TRAIN = 'test_train',
                        FIELD_CATEGORY_NUMBER = 'categoryNumber',
                        FIELD_QUERY_ASSIGNED_CLUSTER = 'assignedClass',
                        FIELD_DOCUMENT_ID = 'document_id'

    static final Analyzer analyzer = new StandardAnalyzer()
    //new EnglishAnalyzer();  //with stemming  new WhitespaceAnalyzer()

    static void setIndex(IndexEnum indexEnum ) {

        index = indexEnum
        indexSearcher = index.getIndexSearcher()
        indexReader = indexSearcher.getIndexReader()
        termQueryList =  ImportantTermQueries.getTFIDFTermQueryList(indexReader, 120) asImmutable()
    }
}