package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

@CompileStatic
class QueryBuilders {

     static BooleanQuery.Builder[] getMultiWordQuery(int[] intChromosome, List<TermQuery> termQueryList, final int k,  BooleanClause.Occur booleanClauseOccur = BooleanClause.Occur.SHOULD) {

        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder [k]
        Set<Integer> alleles = [] as Set<Integer>

        int gene = 0
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

        //add additional words to the queries if intersect requirement is met
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
}