package cluster;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EsqQueryBuilder {

    private static final int MAX_INTERSECT_LIST_SIZE = 2;

    private final List<TermQuery> tql;
    private final Map<String, List<TermQuery>> orderedIntersectMap;
    private final BooleanClause.Occur booleanClauseOccur;
    private final EsqQueryBuilderMethod builderMethod;

    public EsqQueryBuilder(List<TermQuery> termQueryList, Map<String, List<TermQuery>> orderedIntersectMap, EsqQueryBuilderMethod eSQbm, BooleanClause.Occur bco) {
        this.tql = termQueryList;
        this.booleanClauseOccur = bco;
        this.orderedIntersectMap = orderedIntersectMap;
        this.builderMethod = eSQbm;
    }

    public EsqQueryBuilder(List<TermQuery> termQueryList, Map<String, List<TermQuery>> orderedIntersectMap, EsqQueryBuilderMethod eSQbm) {
        this(termQueryList, orderedIntersectMap, eSQbm, BooleanClause.Occur.SHOULD);
    }

    public BooleanQuery.Builder[] buildQueries(final Genotype<IntegerGene> gt, final int k) throws java.io.IOException {
        switch (builderMethod) {
            case SINGLE:
                return getSingleWordQueries(((IntegerChromosome) gt.get(1)).toArray(), k);
            case INTERSECT:
                return getIntersectQueries(((IntegerChromosome) gt.get(1)).toArray(), ((IntegerChromosome) gt.get(2)).toArray(), k);
            case BLOCKS:
                return getMultiWordQueryBlocks(((IntegerChromosome) gt.get(3)).toArray(), k);
            case MODULUS:
                return getMultiWordQueryModulusDuplicateCheck(((IntegerChromosome) gt.get(3)).toArray(), k);
            default:
                throw new IllegalStateException("Unexpected BuilderMethod: " + builderMethod);
        }
    }

    public BooleanQuery.Builder[] getSingleWordQueries(int[] intChromosome, final int k) {
        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder[k];
        for (int i = 0; i < k; i++) {
            final int allele = intChromosome[i];
            arrayOfBuilders[i] = new BooleanQuery.Builder().add(tql.get(allele), booleanClauseOccur);
        }
        return arrayOfBuilders;
    }

    public BooleanQuery.Builder[] getIntersectQueries(int[] rootChromosome, int[] intersectChromosome, final int k) {
        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder[k];

        for (int i = 0; i < k; i++) {
            final int rootAllele = rootChromosome[i];
            arrayOfBuilders[i] = new BooleanQuery.Builder().add(tql.get(rootAllele), booleanClauseOccur);
            String rootWord = tql.get(rootAllele).getTerm().text();
            List<TermQuery> intersectTermQueryList = orderedIntersectMap.get(rootWord);

            Set<Integer> intersectAlleles = new HashSet<>();
            int startIndex = i * MAX_INTERSECT_LIST_SIZE;
            for (int j = startIndex; j < intersectChromosome.length; j++) {
                final int intersectAllele = intersectChromosome[j];
                if (intersectAllele >= 0 && intersectTermQueryList != null && intersectAlleles.add(intersectAllele) && intersectAllele < intersectTermQueryList.size()) {
                    arrayOfBuilders[i].add(intersectTermQueryList.get(intersectAllele), booleanClauseOccur);
                }
            }
        }
        return arrayOfBuilders;
    }

    public BooleanQuery.Builder[] getMultiWordQueryBlocks(int[] intChromosome, final int k) throws java.io.IOException {
        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder[k];
        int clusterNumber = -1;
        TermQuery tqRoot = null;
        int blockSize = intChromosome.length / k;

        for (int i = 0; i < intChromosome.length; i++) {
            final int allele = intChromosome[i];

            if (i % blockSize == 0) {
                clusterNumber++;
                if (clusterNumber >= k) {
                    clusterNumber = 0;
                }
                tqRoot = tql.get(allele);
                if (tqRoot == null) {
                    continue;
                }
                arrayOfBuilders[clusterNumber] = new BooleanQuery.Builder().add(tqRoot, booleanClauseOccur);
            } else {
                TermQuery tqNew = tql.get(allele);
                if (tqRoot != null && QueryTermIntersectRatio.isValidIntersect(tqRoot, tqNew)) {
                    arrayOfBuilders[clusterNumber].add(tqNew, booleanClauseOccur);
                }
            }
        }
        return arrayOfBuilders;
    }

    public BooleanQuery.Builder[] getMultiWordQueryModulusDuplicateCheck(int[] intChromosome, final int k) throws java.io.IOException {
        BooleanQuery.Builder[] arrayOfBuilders = new BooleanQuery.Builder[k];
        Set<Integer> alleles = new HashSet<>();

        int gene = 0;
        int uniqueWords = 0;

        while (uniqueWords < k && gene < intChromosome.length) {
            final int allele = intChromosome[gene];
            if (alleles.add(allele)) {
                arrayOfBuilders[uniqueWords] = new BooleanQuery.Builder().add(tql.get(allele), booleanClauseOccur);
                uniqueWords++;
            }
            gene++;
        }

        for (int j = gene; j < intChromosome.length; j++) {
            final int allele = intChromosome[j];
            final int clusterNumber = j % k;

            BooleanQuery rootq = arrayOfBuilders[clusterNumber].build();
            Query tq0 = rootq.clauses().get(0).query();
            TermQuery tqNew = tql.get(allele);

            if (alleles.add(allele) && QueryTermIntersectRatio.isValidIntersect(tq0, tqNew)) {
                arrayOfBuilders[clusterNumber].add(tqNew, booleanClauseOccur);
            }
        }

        return arrayOfBuilders;
    }
}
