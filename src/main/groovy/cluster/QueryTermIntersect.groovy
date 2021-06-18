package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.*

@CompileStatic
class QueryTermIntersect {

    static boolean isValidIntersect(Query q0, Query q1) {
        return getIntersectValue(q0,q1) >= Indexes.MIN_INTERSECT_RATIO
    }

    static double getIntersectValue(Query q0, Query q1) {
        IndexSearcher indexSearcher = Indexes.indexSearcher

        TotalHitCountCollector collector = new TotalHitCountCollector();
        BooleanQuery.Builder bqbAnd = new BooleanQuery.Builder();

        bqbAnd.add(q0, BooleanClause.Occur.MUST)
        bqbAnd.add(q1, BooleanClause.Occur.MUST)
        indexSearcher.search(bqbAnd.build(), collector);
        final int andCount = collector.getTotalHits();

        collector = new TotalHitCountCollector();
        indexSearcher.search(q1, collector)
        final int q1Count = collector.getTotalHits()

        assert q1Count > 0
        final double intersectRatio = andCount / q1Count as double

        return intersectRatio
    }
}