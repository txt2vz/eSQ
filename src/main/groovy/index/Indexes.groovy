package index

import groovy.transform.CompileStatic
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic
enum IndexEnum {

    R4('indexes/R4Train', 4),
    R4TEST('indexes/R4Test', 4),
    R5('indexes/R5Train', 5),
    R5TEST('indexes/R5Test', 5),
    R6('indexes/R6Train', 6),
    R6TEST('indexes/R6Test', 6),

    NG3('indexes/NG3Train', 3),
    NG3TEST('indexes/NG3Test', 3),
    NG5('indexes/NG5Train', 5),
    NG5TEST('indexes/NG5Test', 5),
    NG6('indexes/NG6Train', 6),
    NG6TEST('indexes/NG6Test', 6),

    CLASSIC4('indexes/classic4Train', 4),
    CLASSIC4TEST('indexes/classic4Test', 4),

    CRISIS3('indexes/crisis3Train', 3),
    CRISIS3TEST('indexes/crisis3Test', 3)

    // private final Similarity similarity = new BM25Similarity()  // new ClassicSimilarity()
    String pathString
    int numberOfCategories

    IndexEnum(String pathString, int numberOfCategories) {
        this.numberOfCategories = numberOfCategories
        this.pathString = pathString
    }

    String toString() {
        return "${this.name()} path: $pathString numberOfCategories: $numberOfCategories "
    }

    IndexReader getIndexReader() {
        Path path = Paths.get(pathString)
        Directory directory = FSDirectory.open(path)
        IndexReader ir = DirectoryReader.open(directory)
        println "IndexReader: $ir"
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

    // Lucene field names
    static final String FIELD_CATEGORY_NAME = 'category',
                        FIELD_CONTENTS = 'contents',
                        FIELD_PATH = 'path',
                        FIELD_TEST_TRAIN = 'test_train',
                        FIELD_CATEGORY_NUMBER = 'categoryNumber',
                        FIELD_ASSIGNED_CLASS = 'assignedClass',
                        FIELD_DOCUMENT_ID = 'document_id';

    static final Analyzer analyzer = new StandardAnalyzer()  //new EnglishAnalyzer();  //with stemming  new WhitespaceAnalyzer()

    static void setIndex(IndexEnum ie, boolean printDetails = false) {
        index = ie
        indexSearcher = index.getIndexSearcher()
        indexReader = indexSearcher.getIndexReader()

        if (printDetails) {
            println "indexEnum $index maxDocs ${indexReader.maxDoc()}"
        }
    }
}