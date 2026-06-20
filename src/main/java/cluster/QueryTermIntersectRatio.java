package cluster;

import index.Indexes;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;

import java.io.IOException;

public class QueryTermIntersectRatio {

    public static final double MIN_INTERSECT_RATIO = 0.5;

    public static boolean isValidIntersect(Query q0, Query q1) throws IOException {
        return getIntersectValue(q0, q1) >= MIN_INTERSECT_RATIO;
    }

    public static double getIntersectValue(Query q0, Query q1) throws IOException {
        IndexSearcher indexSearcher = Indexes.indexSearcher;

        TotalHitCountCollector collector = new TotalHitCountCollector();
        BooleanQuery.Builder bqbAnd = new BooleanQuery.Builder();
        bqbAnd.add(q0, BooleanClause.Occur.MUST);
        bqbAnd.add(q1, BooleanClause.Occur.MUST);
        indexSearcher.search(bqbAnd.build(), collector);
        int andCount = collector.getTotalHits();

        collector = new TotalHitCountCollector();
        indexSearcher.search(q1, collector);
        int q1Count = collector.getTotalHits();

        if (q1Count == 0) {
            return 0.0;
        }

        return (double) andCount / (double) q1Count;
    }
}
