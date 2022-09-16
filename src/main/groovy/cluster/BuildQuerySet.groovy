package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

//see https://www.tutorialspoint.com/genetic_algorithms/genetic_algorithms_fundamentals.htm

enum QType {
    OR1('Single-Word'),
    OR_INTERSECT('Multi-Word-OR'),
    AND_INTERSECT('Multi-Word-AND')

    QType(String desc) {
        queryDescription = desc
    }

    String queryDescription;

    String description() {
        return queryDescription;
    }
}

@CompileStatic
class BuildQuerySet {

    static List<BooleanQuery.Builder> getQueryBuilderList(int[] intChromosome, final int k, QType qType) {

        switch (qType) {
            case QType.OR1:
                return getOneWordQueryPerCluster(intChromosome, Indexes.termQueryList, k)
                break

            case QType.OR_INTERSECT:
                return getIntersect(intChromosome, Indexes.termQueryList, k, BooleanClause.Occur.SHOULD);
                break

//            case QType.AND_INTERSECT:
//                return getIntersect(intChromosome, termQueryList, k, BooleanClause.Occur.MUST);
//                break
        }
    }

    private static List<BooleanQuery.Builder> getIntersect(int[] intChromosome, List<TermQuery> termQueryList, final int k, BooleanClause.Occur booleanClauseOccur) {

        BooleanQuery.Builder[] bqbL = new BooleanQuery.Builder [k]

     //   List<BooleanQuery.Builder> bqbL = []
        Set<Integer> alleles = [] as Set<Integer>

        int i=0
        int acceptedWords = 0
        while (acceptedWords < k  && i < intChromosome.size()){
            final int allele = intChromosome[i]

            if (alleles.add(allele) ){
                bqbL[acceptedWords] = new BooleanQuery.Builder().add(termQueryList[allele], booleanClauseOccur)
               acceptedWords++
            }
            i++
        }

        for (int j = i; j < intChromosome.size(); j++) {

            final int allele = intChromosome[j]
            final int clusterNumber = j % k

            BooleanQuery rootq = bqbL[clusterNumber].build()
            Query tq0 = rootq.clauses().first().getQuery()
            TermQuery tqNew = termQueryList[allele]

            if (alleles.add(allele) && (QueryTermIntersect.isValidIntersect(tq0, tqNew))) {
                bqbL[clusterNumber].add(tqNew, booleanClauseOccur)
            }
        }

        assert bqbL.size() == k
       // return bqbL.asImmutable()
        return bqbL.toList()
    }

    private static List<BooleanQuery.Builder> getOneWordQueryPerCluster(int[] intChromosome, List<TermQuery> termQueryList, final int k) {

        List<BooleanQuery.Builder> bqbL = []

        for (int i = 0; i < k && i < intChromosome.size(); i++) {

            final int allele = intChromosome[i]
            bqbL[i] = new BooleanQuery.Builder().add(termQueryList[allele], BooleanClause.Occur.SHOULD)
        }

        assert bqbL.size() == k
        return bqbL.asImmutable()
    }

    private static List<BooleanQuery.Builder> getIntersectWithMap(int[] intChromosome, List<TermQuery> termQueryList, final int k, BooleanClause.Occur booleanClauseOccur) {

        List<BooleanQuery.Builder> bqbL = []
        Set<Integer> alleleSet = [] as Set<Integer>

        final int intersectGenomeStart = 9

        for (int i = 0; i < k; i++) {

            final int rootAllele = intChromosome[i]
            TermQuery termQueryRoot = termQueryList[rootAllele]
            bqbL[i] = new BooleanQuery.Builder().add(termQueryRoot, booleanClauseOccur)

            for (int j = i + intersectGenomeStart; j < intChromosome.size(); j = j + k) {
                final int newAllele = intChromosome[j]

             //   assert Indexes.termQueryIntersectMap.containsKey(termQueryRoot)

                List<Tuple2<TermQuery, Double>> intersectingTerms = Indexes.termQueryIntersectMap[termQueryRoot]

                if (intersectingTerms.size() > 0 && newAllele >= 0 && newAllele < intersectingTerms.size() && (alleleSet.add(newAllele))) {

                    TermQuery tqNew = intersectingTerms[newAllele].v1

                    assert bqbL[i].add(tqNew, booleanClauseOccur)
                }
            }
        }

        assert bqbL.size() == k
        return bqbL.asImmutable()
    }

    static String printQuerySet(Map<Query, Integer> queryIntegerMap) {
        StringBuilder sb = new StringBuilder()
        queryIntegerMap.keySet().eachWithIndex { Query q, int index ->
            sb << "ClusterQuery: $index :  ${queryIntegerMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)}  \n"
        }
        return sb.toString()
    }
}