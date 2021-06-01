package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.*

@CompileStatic
enum MinIntersectValue {

    NONE(0.0d),
    RATIO_POINT_1(0.1d),
    RATIO_POINT_2(0.2d),
    RATIO_POINT_3(0.3d),
    RATIO_POINT_4(0.4d),
    RATIO_POINT_5(0.5d),
    RATIO_POINT_6(0.6d),
    RATIO_POINT_7(0.7d),
    RATIO_POINT_8(0.8d),
    RATIO_POINT_9(0.9d),
    RATIO_POINT_95(0.95d),
    RATIO_POINT_97(0.97d),
    RATIO_POINT_10(1.0d)

    MinIntersectValue(double minVal) {
        intersectRatio = minVal
    }
    double intersectRatio
}


@CompileStatic
class QueryTermIntersect {

    static boolean isValidIntersect(Query q0, Query q1, double minIntersect = 0.6d) {
        return getIntersectValue(q0,q1) >minIntersect
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

        return (andCount / q1Count)
    }
}