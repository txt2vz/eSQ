package cluster

import groovy.transform.CompileStatic
import index.Indexes
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
class QueryBuilders {

//    static BooleanQuery.Builder[] getQueryBuilderArray(int[] intChromosome, final int k, QType qType) {
//
//        switch (qType) {
//            case QType.OR1:
//                return getOneWordQueryPerCluster(intChromosome, Indexes.termQueryList, k)
//                break
//
//            case QType.OR_INTERSECT:
//                return getMultiWordQuery(intChromosome, Indexes.termQueryList, k, BooleanClause.Occur.SHOULD);
//                break
//
////            case QType.AND_INTERSECT:
////                return getIntersect(intChromosome, termQueryList, k, BooleanClause.Occur.MUST);
////                break
//        }
//    }

     static BooleanQuery.Builder[] getMultiWordQuery(int[] intChromosome, List<TermQuery> termQueryList, final int k,  BooleanClause.Occur booleanClauseOccur = BooleanClause.Occur.SHOULD) {

        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder [k]
        Set<Integer> alleles = [] as Set<Integer>

      //  int gene = setkJenetics ? 1 : 0
        int gene = 0//JeneticsMain.GA_TO_SETK ? 1 : 0    //if GA setting k then the first element of the chromosome is used to set k
        int uniqueWords = 0

        //populate set of unique root words
        while (uniqueWords < k  && gene < intChromosome.size()){
            final int allele = intChromosome[gene]

            if (alleles.add(allele) ){
                arrayOfBuilders[uniqueWords] = new BooleanQuery.Builder().add(termQueryList[allele], booleanClauseOccur)
                uniqueWords++
            }
            gene++
        }

        for (int j = gene; j < intChromosome.size(); j++) {

            final int allele = intChromosome[j]
            final int clusterNumber = j % k

            BooleanQuery rootq = arrayOfBuilders[clusterNumber].build()
            Query tq0 = rootq.clauses().first().getQuery()
            TermQuery tqNew = termQueryList[allele]

            if (alleles.add(allele) && (QueryTermIntersect.isValidIntersect(tq0, tqNew))) {
                arrayOfBuilders[clusterNumber].add(tqNew, booleanClauseOccur)
            }
        }

        return arrayOfBuilders
    }

     static  BooleanQuery.Builder[] getOneWordQueryPerCluster(int[] intChromosome, List<TermQuery> termQueryList, final int k) {

        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder[k]

        for (int gene = 0; gene < k && gene < intChromosome.size(); gene++) {

            final int allele = intChromosome[gene]
            arrayOfBuilders[gene] = new BooleanQuery.Builder().add(termQueryList[allele], BooleanClause.Occur.SHOULD)
        }

        return arrayOfBuilders
    }
}