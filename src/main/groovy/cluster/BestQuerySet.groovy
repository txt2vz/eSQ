package cluster

import org.apache.lucene.search.Query

class BestQuerySet {

    Map<Query, Integer> qMap = new HashMap<Query, Integer>()
    Query[] queryArray
    int totalDocumentsReturnedByOnlyOneQuery
    int totalHitsAllQueries

    // modified queries which do not return documents returned by any other query
    Query[] nonIntersectingQueries;

    BestQuerySet(Map<Query, Integer> qMap,  int totalUnique, int totalAllQ, Query[] nonIntersectingQ){

        this.qMap = qMap
        queryArray = qMap.keySet().toArray() as Query[]
        totalDocumentsReturnedByOnlyOneQuery = totalUnique
        totalHitsAllQueries = totalAllQ
        nonIntersectingQueries = nonIntersectingQ
    }
}
