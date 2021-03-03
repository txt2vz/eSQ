package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TotalHitCountCollector

@CompileStatic
class UniqueHits {

    static Tuple3<Map<Query, Integer>, Integer, Integer> getUniqueHits(List<BooleanQuery.Builder> bqbList) {
        Map<Query, Integer> qMap = new HashMap<Query, Integer>()
        BooleanQuery.Builder totalHitsBQB = new BooleanQuery.Builder()

        int totalUniqueHits = 0
        for (int i = 0; i < bqbList.size(); i++) {
            Query q = bqbList[i].build()
            totalHitsBQB.add(q, BooleanClause.Occur.SHOULD)

            BooleanQuery.Builder bqbOneCategoryOnly = new BooleanQuery.Builder()
            bqbOneCategoryOnly.add(q, BooleanClause.Occur.SHOULD)

            for (int j = 0; j < bqbList.size(); j++) {
                if (j != i) {
                    bqbOneCategoryOnly.add(bqbList[j].build(), BooleanClause.Occur.MUST_NOT)
                }
            }

            TotalHitCountCollector collector = new TotalHitCountCollector();
            Indexes.indexSearcher.search(bqbOneCategoryOnly.build(), collector);
            final int qUniqueHits = collector.getTotalHits()

            qMap.put(q, qUniqueHits)
            totalUniqueHits += qUniqueHits
        }

        TotalHitCountCollector collector = new TotalHitCountCollector();
        Indexes.indexSearcher.search(totalHitsBQB.build(), collector);
        final int totalHitsAllQueries = collector.getTotalHits();

        return new Tuple3(qMap, totalUniqueHits, totalHitsAllQueries)
    }
}
