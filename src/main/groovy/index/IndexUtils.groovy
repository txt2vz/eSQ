package index

import org.apache.lucene.document.Document
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

    static void main (String[] args){
        Indexes.setIndex(IndexEnum.R5)
        categoryFrequencies(Indexes.indexSearcher , true)
    }

    static Tuple3<String, Integer, Integer> getMostFrequentCategoryForQuery(Query q, boolean printDetails = false) {
        Map<String, Integer> categoryFrequencyMap = [:]
        TopScoreDocCollector collector = TopScoreDocCollector.create(Indexes.indexReader.numDocs());
        Indexes.indexSearcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        hits.each { ScoreDoc sd ->
            final int docId = sd.doc
            Document d = Indexes.indexSearcher.doc(docId)
            String categoryName = d.get(Indexes.FIELD_CATEGORY_NAME)
            final int n = categoryFrequencyMap.get((categoryName)) ?: 0
            categoryFrequencyMap.put((categoryName), n + 1)
        }

        String mostFrequentCategoryName = 'not determined'
        int mostFrequentCategoryHits = 0

        Map.Entry<String, Integer> mostFrequentCategory = categoryFrequencyMap?.max { it?.value }

        if ( mostFrequentCategory) {

             mostFrequentCategoryName = mostFrequentCategory?.key
             mostFrequentCategoryHits = mostFrequentCategory?.value
        }

        if (printDetails) {
            println "CategoryFrequencyMap: $categoryFrequencyMap for query: ${q.toString(Indexes.FIELD_CONTENTS)} mostFrequentCategory: $mostFrequentCategory queryHits ${hits.size()} "
        }

        return new Tuple3<String, Integer, Integer>(mostFrequentCategoryName, mostFrequentCategoryHits, hits.size())
    }

    static Map<String, Integer> categoryFrequencies(IndexSearcher indexSearcher, boolean printDetails = false){

        Query qAll = new MatchAllDocsQuery()
        TopDocs topDocs = indexSearcher.search(qAll, Integer.MAX_VALUE)
        ScoreDoc[] allHits = topDocs.scoreDocs

        Map<String, Integer> assignedCategoryFrequencies = [:]
        Map<String, Integer> categoryFrequencies = [:]

        for (ScoreDoc sd : allHits) {
            Document d = indexSearcher.doc(sd.doc)

            String category = d.get(Indexes.FIELD_CATEGORY_NAME)
            String assignedCat = d.get(Indexes.FIELD_ASSIGNED_CLASS)

            int n = assignedCategoryFrequencies.get(assignedCat) ?: 0
            assignedCategoryFrequencies.put((assignedCat), n + 1)

            n = categoryFrequencies.get(category)?: 0
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
