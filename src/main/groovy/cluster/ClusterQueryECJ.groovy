package cluster

import ec.EvolutionState
import ec.Individual
import ec.Problem
import ec.simple.SimpleFitness
import ec.simple.SimpleProblemForm
import ec.util.Parameter
import ec.vector.IntegerVectorIndividual
import groovy.transform.CompileStatic
import index.ImportantTermQueries
import index.Indexes
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery


@CompileStatic
public class ClusterQueryECJ extends Problem implements SimpleProblemForm {

    List <TermQuery>  tql
    static QType QUERY_TYPE

    public void setup(final EvolutionState state, final Parameter base) {

        super.setup(state, base);
        println "ClusterQueryECJ Setup. Total docs for ClusterQueryECJ.groovy   " + Indexes.indexReader.numDocs()
        tql = ImportantTermQueries.getTFIDFTermQueryList(Indexes.indexReader) asImmutable()
    }


    public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
                         final int threadnum) {

        if (ind.evaluated)
            return;

        ECJclusterFitness fitness = (ECJclusterFitness) ind.fitness;
        IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind

        final int[] genomeOrig = intVectorIndividual.genome as int[]
        final int   k =  (ClusterMainECJ.SETK) ? genomeOrig[0] :  Indexes.index.numberOfCategories

        final int[] genome = (ClusterMainECJ.SETK) ? genomeOrig[1.. genomeOrig.size()-1] as int[] : genomeOrig

        List<BooleanQuery.Builder> bqbList = QuerySet.getQueryBuilderList(genome, tql, k, QUERY_TYPE);
        Tuple3<Map<Query, Integer>, Integer, Integer> uniqueHitsTuple = UniqueHits.getUniqueHits(bqbList);

       // final int uniqueHits = uniqueHitsTuple.v2
        final int uniqueHits = uniqueHitsTuple.v2 - (uniqueHitsTuple.v3 - uniqueHitsTuple.v2)
        final double f = (ClusterMainECJ.SETK) ? uniqueHits * (1.0 - (0.04 * k)) as double : uniqueHits as double
        final double rawfitness= (f > 0) ? f : 0.0d;

        fitness.setClusterFitness(uniqueHitsTuple, bqbList, rawfitness )

        ((SimpleFitness) intVectorIndividual.fitness).setFitness(state, rawfitness, false)
        ind.evaluated = true
    }
}