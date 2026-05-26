package index

import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.StoredFields
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopDocs
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.search.TotalHitCountCollector

class IndexUtils {

    static void main(String[] args) {
        Indexes.setIndex(IndexEnum.R5)
        categoryFrequencies(Indexes.indexSearcher, true)
    }

    static Map<String, Integer> categoryFrequencies(IndexReader indexReader, boolean printDetails = false) {

        // Get the IndexReader from the IndexSearcher
        //IndexReader reader = Indexes.indexSearcher.getIndexReader();
        IndexSearcher indexSearcher = new IndexSearcher(indexReader)

// Get the StoredFields instance from the reader
        StoredFields storedFields = indexReader.storedFields()

        Query qAll = new MatchAllDocsQuery()
        TopDocs topDocs = indexSearcher.search(qAll, Integer.MAX_VALUE)
        ScoreDoc[] allHits = topDocs.scoreDocs

        Map<String, Integer> assignedCategoryFrequencies = [:]
        Map<String, Integer> categoryFrequencies = [:]

        for (ScoreDoc sd : allHits) {
         //   Document d = indexSearcher.doc(sd.doc)
            Document d = storedFields.document(sd.doc);

            String category = d.get(Indexes.FIELD_CATEGORY_NAME)
            String assignedCat = d.get(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER)

            int n = assignedCategoryFrequencies.get(assignedCat) ?: 0
            assignedCategoryFrequencies.put((assignedCat), n + 1)

            n = categoryFrequencies.get(category) ?: 0
            categoryFrequencies.put((category), n + 1)
        }

        if (printDetails) {
            println "Index Utils assingedCatFrequency $assignedCategoryFrequencies"
            println "category freq $categoryFrequencies"
        }

        return categoryFrequencies
    }

    //get hits for a particular query using filter (e.g. a particular category)
    static int getQueryHitsWithFilter(IndexSearcher searcher, Query filter, Query q) {
        TotalHitCountCollector collector = new TotalHitCountCollector()
        BooleanQuery.Builder bqb = new BooleanQuery.Builder()
        bqb.add(q, BooleanClause.Occur.MUST)
        bqb.add(filter, BooleanClause.Occur.FILTER)
        searcher.search(bqb.build(), collector)
        return collector.getTotalHits()
    }
}
