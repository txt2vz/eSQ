package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

//see https://www.tutorialspoint.com/genetic_algorithms/genetic_algorithms_fundamentals.htm

enum QType {
    OR1 ('Single-Word'),
    OR_INTERSECT ('Multi-Word-OR'),
    AND_INTERSECT ('Multi-Word-AND')

    QType(String desc){
        queryDescription=desc
    }

    String queryDescription;
    String description(){
        return queryDescription;
    }
}

@CompileStatic
class QuerySet {

    static List<BooleanQuery.Builder> getQueryBuilderList(int[] intChromosome, List<TermQuery> termQueryList, final int k, QType qType) {
        //  MinIntersectValue mi =

        switch (qType) {
            case QType.OR1: return getOneWordQueryPerCluster(intChromosome, termQueryList, k)
                break

            case QType.OR_INTERSECT:
                return getIntersect(intChromosome, termQueryList, k, BooleanClause.Occur.SHOULD);
                break

            case QType.AND_INTERSECT:
                return getIntersect(intChromosome, termQueryList, k, BooleanClause.Occur.MUST);
                break
        }
    }

    private static List<BooleanQuery.Builder> getOneWordQueryPerCluster(int[] intChromosome, List<TermQuery> termQueryList, final int k) {

        //   Set<Integer> alleles = [] as Set<Integer>
        List<BooleanQuery.Builder> bqbL = []

        int index = 0;
        int clusterNumber = 0
        while (clusterNumber < k && index < intChromosome.size()) {

            final int allele = intChromosome[index]
            assert allele < termQueryList.size() && allele >= 0

            //   if (alleles.add(allele)) {
            bqbL[clusterNumber] = new BooleanQuery.Builder().add(termQueryList[allele], BooleanClause.Occur.SHOULD)
            clusterNumber++
            //   }
            index++
        }
        return bqbL.asImmutable()
    }

    private static List<BooleanQuery.Builder> getIntersect(int[] intChromosome, List<TermQuery> termQueryList, final int k, BooleanClause.Occur booleanClauseOccur) {

        List<BooleanQuery.Builder> bqbL = []
        Set<Integer> alleles = [] as Set<Integer>
        int clusterNumber = 0
        int index = 0

        while (clusterNumber < k && index < intChromosome.size()) {
            final int allele = intChromosome[index]

            if (alleles.add(allele)) {
                bqbL[clusterNumber] = new BooleanQuery.Builder().add(termQueryList[allele], booleanClauseOccur )
                clusterNumber++
            }
            index++
        }

        for (int i = index; i < intChromosome.size(); i++) {

            final int allele = intChromosome[i]
            clusterNumber = i % k

            BooleanQuery rootq = bqbL[clusterNumber].build()
            Query tq0 = rootq.clauses().first().getQuery()
            TermQuery tqNew = termQueryList[allele]

            if (alleles.add(allele) && (QueryTermIntersect.isValidIntersect(tq0, tqNew))) {
                bqbL[clusterNumber].add(tqNew, booleanClauseOccur)
            }
        }

        assert bqbL.size() == k
        return bqbL.asImmutable()
    }

    static Tuple6<Map<Query, Integer>, Integer, Integer, Double, Double, Double> querySetInfo(List<BooleanQuery.Builder> bqbList, boolean printQueries = false) {

        Tuple3<Map<Query, Integer>, Integer, Integer> t3 = UniqueHits.getUniqueHits(bqbList);

        Map<Query, Integer> queryMap = t3.v1
        final int uniqueHits = t3.v2
        final int totalHitsAllQueries = t3.v3

        Tuple4<Double, Double, Double, List<Double>> t4QuerySetEffectiveness = Effectiveness.querySetEffectiveness(queryMap.keySet());
        final double f1 = t4QuerySetEffectiveness.v1
        final double precision = t4QuerySetEffectiveness.v2
        final double recall = t4QuerySetEffectiveness.v3

        if (printQueries) {
            println printQuerySet(queryMap);
        }

        return new Tuple6(queryMap, uniqueHits, totalHitsAllQueries, f1, precision, recall)
    }

    static String printQuerySet(Map<Query, Integer> queryIntegerMap) {
        StringBuilder sb = new StringBuilder()
        queryIntegerMap.keySet().eachWithIndex { Query q, int index ->
            sb << "ClusterQuery: $index :  ${queryIntegerMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)}  \n"
        }
        return sb.toString()
    }
}