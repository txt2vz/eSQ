package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TotalHitCountCollector

@CompileStatic
class QuerysetFeatures {

    static Tuple4<Map<Query, Integer>, Integer, Integer, Query[]> getQuerysetFeatures(BooleanQuery.Builder[] arrayOfQueryBuilders) {
        Map<Query, Integer> qMap = new HashMap<Query, Integer>()
        BooleanQuery.Builder totalHitsBQB = new BooleanQuery.Builder()

       // modified queries which do not return documents returned by any other query
        Query[] nonIntersectingQueries = new Query[arrayOfQueryBuilders.size()]

        int totalDocumentsReturnedByOnlyOneQuery = 0
        for (int i = 0; i < arrayOfQueryBuilders.size(); i++) {
            Query q = arrayOfQueryBuilders[i].build()

            totalHitsBQB.add(q, BooleanClause.Occur.SHOULD)

            BooleanQuery.Builder builderForNonIntersectingQuery = new BooleanQuery.Builder()
            builderForNonIntersectingQuery.add(q, BooleanClause.Occur.SHOULD)

            for (int j = 0; j < arrayOfQueryBuilders.size(); j++) {
                if (j != i) {
                    builderForNonIntersectingQuery.add(arrayOfQueryBuilders[j].build(), BooleanClause.Occur.MUST_NOT)
                }
            }

            TotalHitCountCollector distinctHitCollector = new TotalHitCountCollector()
            nonIntersectingQueries[i] = builderForNonIntersectingQuery.build()
            Indexes.indexSearcher.search(nonIntersectingQueries[i], distinctHitCollector)
            final int qDistinctHits = distinctHitCollector.getTotalHits()

            qMap.put(q, qDistinctHits)
            totalDocumentsReturnedByOnlyOneQuery += qDistinctHits
        }

        TotalHitCountCollector collector = new TotalHitCountCollector();
        Indexes.indexSearcher.search(totalHitsBQB.build(), collector);
        final int totalHitsAllQueries = collector.getTotalHits();

        return new Tuple4(qMap, totalDocumentsReturnedByOnlyOneQuery, totalHitsAllQueries, nonIntersectingQueries)
    }
}
