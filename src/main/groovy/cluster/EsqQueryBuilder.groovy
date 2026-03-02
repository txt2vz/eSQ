package cluster

import groovy.transform.CompileStatic
import io.jenetics.Genotype
import io.jenetics.IntegerChromosome
import io.jenetics.IntegerGene
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

enum BuilderMethod {
    INTERSECT,
    BLOCKS,
    SINGLE,
    MODULUS
}

@CompileStatic
class EsqQueryBuilder {
    List<TermQuery> tql
    Map<String, List<TermQuery>> orderedIntersectMap
    BooleanClause.Occur booleanClauseOccur
    BuilderMethod builderMethod

    EsqQueryBuilder(List<TermQuery> termQueryList, Map<String, List<TermQuery>> orderedIntersectMap, BuilderMethod bm, BooleanClause.Occur bco = BooleanClause.Occur.SHOULD) {
        tql = termQueryList
        booleanClauseOccur = bco
        this.orderedIntersectMap = orderedIntersectMap
        this.builderMethod = bm
    }

    BooleanQuery.Builder[] buildQueries(final Genotype<IntegerGene> gt, final int k) {
        switch (builderMethod) {
            case BuilderMethod.SINGLE -> getSingleWordQueries(((IntegerChromosome) gt.get(1)).toArray(), k)
            case BuilderMethod.INTERSECT -> getIntersectQueries(((IntegerChromosome) gt.get(1)).toArray(), ((IntegerChromosome) gt.get(2)).toArray(), k)
            case BuilderMethod.BLOCKS -> getMultiWordQueryBlocks(((IntegerChromosome) gt.get(3)).toArray(), k)
            case BuilderMethod.MODULUS -> getMultiWordQueryModulusDuplicateCheck(((IntegerChromosome) gt.get(3)).toArray(), k)
        }
    }

    BooleanQuery.Builder[] getSingleWordQueries(int[] intChromosome, final int k) {
        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder[k]
        for (int i = 0; i < k; i++) {

            final int allele = intChromosome[i]
            arrayOfBuilders[i] = new BooleanQuery.Builder().add(tql[allele], booleanClauseOccur)
        }
        return arrayOfBuilders
    }

    BooleanQuery.Builder[] getIntersectQueries(int[] rootChromosome, int[] intersectChromosome, final int k) {

        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder[k]

        for (int i = 0; i < k; i++) {
            final int rootAllele = rootChromosome[i]
            arrayOfBuilders[i] = new BooleanQuery.Builder().add(tql[rootAllele], booleanClauseOccur)
            String rootWord = tql[rootAllele].term.text()
            List<TermQuery> intersectTermQueryList = orderedIntersectMap[rootWord]

            Set<Integer> intersectAlleles = [] as Set<Integer>
            for (int j = i * JeneticsMain.maxIntersectListSize; j < intersectChromosome.size(); j++) {
                final int intersectAllele = intersectChromosome[j]
                if (intersectAllele >= 0 && intersectTermQueryList && intersectAlleles.add(intersectAllele) && intersectAllele < intersectTermQueryList.size()) {
                    arrayOfBuilders[i] = arrayOfBuilders[i].add(intersectTermQueryList[intersectAllele], booleanClauseOccur)
                }
            }
        }

        return arrayOfBuilders
    }

    //divide chromosome into blocks depending on k.  May help evolution?
    BooleanQuery.Builder[] getMultiWordQueryBlocks(int[] intChromosome, final int k) {

        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder[k]
        int clusterNumber = -1
        TermQuery tqRoot
        int blockSize = (int) (intChromosome.length / k)

        for (int i = 0; i < intChromosome.size(); i++) {

            final int allele = intChromosome[i]

            if (i % blockSize == 0) {
                clusterNumber++
                if (clusterNumber >= k) {
                    clusterNumber = 0
                }

                tqRoot = tql[allele]
                assert tqRoot != null
                arrayOfBuilders[clusterNumber] = new BooleanQuery.Builder().add(tqRoot, booleanClauseOccur)
            } else {
                TermQuery tqNew = tql[allele]

                if (QueryTermIntersectRatio.isValidIntersect(tqRoot, tqNew)) {
                    arrayOfBuilders[clusterNumber].add(tqNew, booleanClauseOccur)
                }
            }
        }
        assert arrayOfBuilders.length <= k
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
            Query tq0 = rootq.clauses().first().query()
            TermQuery tqNew = tql[allele]

            if (alleles.add(allele) && (QueryTermIntersectRatio.isValidIntersect(tq0, tqNew))) {
                arrayOfBuilders[clusterNumber].add(tqNew, booleanClauseOccur)
            }
        }

        return arrayOfBuilders
    }
}