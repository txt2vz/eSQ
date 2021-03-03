package cluster

import ec.EvolutionState
import ec.simple.SimpleStatistics
import groovy.transform.CompileStatic
import index.Indexes

@CompileStatic
public class ClusterStatisticsECJ extends SimpleStatistics {

    public void finalStatistics(final EvolutionState state, final int result) {
        // print out the other statistics
        super.finalStatistics(state, result);
    }

    public void postEvaluationStatistics(EvolutionState state) {
        super.postEvaluationStatistics(state);

        ECJclusterFitness cf = (ECJclusterFitness) state.population.subpops.collect { sbp ->
            sbp.individuals.max() { ind ->
                ind.fitness.fitness()
            }.fitness
        }.max { it.fitness() }


        if (state.generation % 5 == 0) generationReport(state, cf)

        cf.generationStats(state.generation)
    }

    private void generationReport(EvolutionState state, ECJclusterFitness cfit) {

        Tuple4 tuple4 = Effectiveness.querySetEffectiveness(cfit.queryMap.keySet())

        final double averageF1 = tuple4.first
        final double averagePrecision = tuple4.second
        final double averageRecall  = tuple4.third

        File fcsv = new File('results/generationReport.csv')
        if (!fcsv.exists()) {
            fcsv << 'generation, averageF1, averagePrecision, averageRecall, baseFitness, indexName, fitnessMethod, intersectMethod, queryType, date \n'
        }
    }
}