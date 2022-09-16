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

  //  static List<BooleanQuery.Builder> getQueryBuilderList(int[] intChromosome, final int k, QType qType) {
    static BooleanQuery.Builder[] getQueryBuilderList(int[] intChromosome, final int k, QType qType) {

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

    private static BooleanQuery.Builder[]  getIntersect(int[] intChromosome, List<TermQuery> termQueryList, final int k, BooleanClause.Occur booleanClauseOccur) {

        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder [k]

     //   List<BooleanQuery.Builder> arrayOfBuilders = []
        Set<Integer> alleles = [] as Set<Integer>

        int i=0
        int acceptedWords = 0
        while (acceptedWords < k  && i < intChromosome.size()){
            final int allele = intChromosome[i]

            if (alleles.add(allele) ){
                arrayOfBuilders[acceptedWords] = new BooleanQuery.Builder().add(termQueryList[allele], booleanClauseOccur)
               acceptedWords++
            }
            i++
        }

        for (int j = i; j < intChromosome.size(); j++) {

            final int allele = intChromosome[j]
            final int clusterNumber = j % k

            BooleanQuery rootq = arrayOfBuilders[clusterNumber].build()
            Query tq0 = rootq.clauses().first().getQuery()
            TermQuery tqNew = termQueryList[allele]

            if (alleles.add(allele) && (QueryTermIntersect.isValidIntersect(tq0, tqNew))) {
                arrayOfBuilders[clusterNumber].add(tqNew, booleanClauseOccur)
            }
        }

        assert arrayOfBuilders.size() == k
        return arrayOfBuilders  //.asImmutable()
      //  return arrayOfBuilders.toList()
    }

    private static  BooleanQuery.Builder[] getOneWordQueryPerCluster(int[] intChromosome, List<TermQuery> termQueryList, final int k) {

       // List<BooleanQuery.Builder> arrayOfBuilders = []
        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder [k]

        for (int i = 0; i < k && i < intChromosome.size(); i++) {

            final int allele = intChromosome[i]
            arrayOfBuilders[i] = new BooleanQuery.Builder().add(termQueryList[allele], BooleanClause.Occur.SHOULD)
        }

        assert arrayOfBuilders.size() == k
        return arrayOfBuilders
    }

    static String printQuerySet(Map<Query, Integer> queryIntegerMap) {
        StringBuilder sb = new StringBuilder()
        queryIntegerMap.keySet().eachWithIndex { Query q, int index ->
            sb << "ClusterQuery: $index :  ${queryIntegerMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)}  \n"
        }
        return sb.toString()
    }
}