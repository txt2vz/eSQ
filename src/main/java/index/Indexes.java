package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Indexes {

    public static IndexEnum index;
    public static IndexSearcher indexSearcher;
    public static IndexReader indexReader;
    public static List<TermQuery> termQueryList;
    public static Map<String, List<TermQuery>> orderedIntersectMap;

    public static final String FIELD_CATEGORY_NAME = "category";
    public static final String FIELD_CONTENTS = "contents";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_TEST_TRAIN = "test_train";
    public static final String FIELD_CATEGORY_NUMBER = "categoryNumber";
    public static final String FIELD_QUERY_ASSIGNED_CLUSTER = "assignedClass";
    public static final String FIELD_DOCUMENT_ID = "document_id";

    public static final Analyzer analyzer = //new EnglishAnalyzer(); 
                                            new HashtagPreservingAnalyzer();
                                      //     new StandardAnalyzer();

    public static void setIndex(IndexEnum indexEnum) throws IOException {
        index = indexEnum;
        indexSearcher = index.getIndexSearcher();
        indexReader = indexSearcher.getIndexReader();
    }

    public static void setImportantTermQueryList(int maxSize) throws IOException {
        termQueryList = Collections.unmodifiableList(ImportantTermQueries.getTFIDFTermQueryList(indexReader, maxSize));
        MapWordToIntersectingTermQueryList mapWordToIntersectingTermQueryList = new MapWordToIntersectingTermQueryList();
        orderedIntersectMap = mapWordToIntersectingTermQueryList.getIntersectingTerms(termQueryList);
    }
}
