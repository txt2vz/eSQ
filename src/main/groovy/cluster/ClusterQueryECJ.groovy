package cluster

import ec.EvolutionState
import ec.Individual
import ec.Problem
import ec.simple.SimpleFitness
import ec.simple.SimpleProblemForm
import ec.util.Parameter
import ec.vector.IntegerVectorIndividual
import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery


@CompileStatic
public class ClusterQueryECJ extends Problem implements SimpleProblemForm {

    Map<TermQuery, List<TermQuery>> termIntersectMap
    static QType QUERY_TYPE

    public void setup(final EvolutionState state, final Parameter base) {

        super.setup(state, base);
        println "ClusterQueryECJ Setup. Total docs for ClusterQueryECJ.groovy   " + Indexes.indexReader.numDocs()

    }


    public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
                         final int threadnum) {

        if (ind.evaluated)
            return;

        ClusterFitnessECJ fitness = (ClusterFitnessECJ) ind.fitness;
        IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind

        final int[] genomeOrig = intVectorIndividual.genome as int[]
        final int   k = (ClusterMainECJ.GA_TO_SETK) ? genomeOrig[0] :  Indexes.index.numberOfCategories
        final int[] genome = (ClusterMainECJ.GA_TO_SETK) ? genomeOrig[1.. genomeOrig.size()-1] as int[] : genomeOrig

        BooleanQuery.Builder[] arrayOfQueryBuilders = BuildQuerySet.getQueryBuilderArray(genome, k, QUERY_TYPE) //.toList()
        Tuple4<Map<Query, Integer>, Integer, Integer, Query[]> uniqueHitsTuple = DistinctHits.distinctQueries(arrayOfQueryBuilders);

        final int uniqueHits = uniqueHitsTuple.v2 //- (uniqueHitsTuple.v3 - uniqueHitsTuple.v2)

        final double f = (ClusterMainECJ.GA_TO_SETK) ? uniqueHits * (1.0 - (Indexes.K_PENALTY * k)) as double : uniqueHits as double

        final double rawfitness= (f > 0) ? f : 0.0d;

        fitness.setClusterFitness(uniqueHitsTuple, arrayOfQueryBuilders, rawfitness )

        ((SimpleFitness) intVectorIndividual.fitness).setFitness(state, rawfitness, false)
        ind.evaluated = true
    }
}