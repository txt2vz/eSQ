package index;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IndexUtils {

    public static void main(String[] args) throws IOException {
        Indexes.setIndex(IndexEnum.R5);
        categoryFrequencies(Indexes.indexSearcher.getIndexReader(), true);
    }

    public static Map<String, Integer> categoryFrequencies(IndexReader indexReader, boolean printDetails) throws IOException {
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        StoredFields storedFields = indexReader.storedFields();

        Query qAll = new MatchAllDocsQuery();
        TopDocs topDocs = indexSearcher.search(qAll, Integer.MAX_VALUE);
        ScoreDoc[] allHits = topDocs.scoreDocs;

        Map<String, Integer> assignedCategoryFrequencies = new HashMap<>();
        Map<String, Integer> categoryFrequencies = new HashMap<>();

        for (ScoreDoc sd : allHits) {
            Document d = storedFields.document(sd.doc);
            String category = d.get(Indexes.FIELD_CATEGORY_NAME);
            String assignedCat = d.get(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER);

            assignedCategoryFrequencies.put(assignedCat, assignedCategoryFrequencies.getOrDefault(assignedCat, 0) + 1);
            categoryFrequencies.put(category, categoryFrequencies.getOrDefault(category, 0) + 1);
        }

        if (printDetails) {
            System.out.println("Index Utils assignedCatFrequency " + assignedCategoryFrequencies);
            System.out.println("category freq " + categoryFrequencies);
        }

        return categoryFrequencies;
    }

    public static int getQueryHitsWithFilter(IndexSearcher searcher, Query filter, Query q) throws IOException {
        TotalHitCountCollector collector = new TotalHitCountCollector();
        BooleanQuery.Builder bqb = new BooleanQuery.Builder();
        bqb.add(q, BooleanClause.Occur.MUST);
        bqb.add(filter, BooleanClause.Occur.FILTER);
        searcher.search(bqb.build(), collector);
        return collector.getTotalHits();
    }
}
