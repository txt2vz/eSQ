package cluster

import groovy.transform.CompileStatic
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

enum BuilderMethod {
    BLOCKS,
    MODULUS
}

@CompileStatic
class EsqQueryBuilder {
    List<TermQuery> tql;
    BooleanClause.Occur booleanClauseOccur
    BuilderMethod builderMethod

    EsqQueryBuilder(List<TermQuery> termQueryList, BuilderMethod bm, BooleanClause.Occur bco = BooleanClause.Occur.SHOULD) {
        tql = termQueryList
        booleanClauseOccur = bco
        this.builderMethod = bm
    }

    BooleanQuery.Builder[] buildQueries(int[] intChromosome, final int k) {
        switch (builderMethod) {
            case BuilderMethod.BLOCKS:
                return getMultiWordQueryBlocks(intChromosome, k)
                break

            case BuilderMethod.MODULUS:
                return getMultiWordQueryModulusDuplicateCheck(intChromosome, k)
                break
        }
    }

    //divide chromosome into blocks depending on k.  May help evolution?
    BooleanQuery.Builder[] getMultiWordQueryBlocks(int[] intChromosome, final int k) {

        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder[k]
        int clusterNumber = -1;
        TermQuery tqRoot
        int blockSize = (int) (intChromosome.length / k)

        for (int i = 0; i < intChromosome.size(); i++) {

            final int allele = intChromosome[i]

            if (i % blockSize == 0) {
                clusterNumber++
                if (clusterNumber >= k){
                    clusterNumber = 0
                }

                tqRoot = tql[allele]
                assert tqRoot != null
                arrayOfBuilders[clusterNumber] = new BooleanQuery.Builder().add(tqRoot, booleanClauseOccur)
            } else {
                TermQuery tqNew = tql[allele]

                if (QueryTermIntersect.isValidIntersect(tqRoot, tqNew)) {
                    arrayOfBuilders[clusterNumber].add(tqNew, booleanClauseOccur)
                }
            }
        }
        assert arrayOfBuilders.length <=k
        return arrayOfBuilders
    }

//use modulus to determine which gene is for which query.  Do not repeat words
     BooleanQuery.Builder[] getMultiWordQueryModulusDuplicateCheck(int[] intChromosome, final int k) {

        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder[k]
        Set<Integer> alleles = [] as Set<Integer>

        int gene = 0
        int uniqueWords = 0

        //populate set of unique root words
        while (uniqueWords < k && gene < intChromosome.size()) {
            final int allele = intChromosome[gene]

            if (alleles.add(allele)) {
                arrayOfBuilders[uniqueWords] = new BooleanQuery.Builder().add(tql[allele], booleanClauseOccur)
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
            TermQuery tqNew = tql[allele]

            if (alleles.add(allele) && (QueryTermIntersect.isValidIntersect(tq0, tqNew))) {
                arrayOfBuilders[clusterNumber].add(tqNew, booleanClauseOccur)
            }
        }

        return arrayOfBuilders
    }
}