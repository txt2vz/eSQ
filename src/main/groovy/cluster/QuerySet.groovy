package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TotalHitCountCollector

@CompileStatic
class QuerySet {

    //holds the original query and the result of running the non-intersecting query
    Map<Query, Integer> queryMap
    Query[] queryArray
    int totalHitsReturnedByOnlyOneQuery
    int totalHitsAllQueries

    // modified queries which do not return documents returned by any other query
    Query[] nonIntersectingQueries

    QuerySet(BooleanQuery.Builder[] arrayOfQueryBuilders){

        queryMap = new HashMap<Query, Integer>()
        queryArray = new Query[arrayOfQueryBuilders.size()]
        BooleanQuery.Builder totalHitsBQB = new BooleanQuery.Builder()

        // modified queries which do not return documents returned by any other query
        nonIntersectingQueries = new Query[arrayOfQueryBuilders.size()]

        totalHitsAllQueries = 0
        totalHitsReturnedByOnlyOneQuery =0

        for (int i = 0; i < arrayOfQueryBuilders.size(); i++) {
            if (arrayOfQueryBuilders[i] == null) println "null and i is " + i
            Query q = arrayOfQueryBuilders[i].build()
            queryArray[i] = q

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

            queryMap.put(q, qDistinctHits)
            totalHitsReturnedByOnlyOneQuery += qDistinctHits
        }

        TotalHitCountCollector collector = new TotalHitCountCollector();
        Indexes.indexSearcher.search(totalHitsBQB.build(), collector);
        totalHitsAllQueries = collector.getTotalHits();
    }
}
