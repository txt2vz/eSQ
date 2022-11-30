package cluster


import classify.LuceneClassifyMethod
import classify.Classify
import ec.EvolutionState
import ec.Evolve
import ec.util.ParameterDatabase
import ec.util.Parameter
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.CompileStatic
import index.IndexEnum
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query

@CompileStatic
class ClusterMainECJ extends Evolve {

    final static int NUMBER_OF_JOBS = 3
    final static int MAX_FIT_JOBS = 3
    final static String gaEngine = "ECJ"
    static boolean GA_TO_SETK
    final static boolean useNonIntersectingClustersForTraining = true
    final static int k_for_knn = 10
    //final static boolean queryOnly = true

    List<IndexEnum> indexList = [

//           IndexEnum.CRISIS3,
//           IndexEnum.NG3,
//
//           IndexEnum.CRISIS4,
//           IndexEnum.R4,
//
//           IndexEnum.NG5,
//           IndexEnum.R5,

           IndexEnum.NG6,
           IndexEnum.R6
    ]

    List<Double> kPenalty = // [0.03d]
            [0.03d]
   //   [0.00d, 0.03d, 0.05d, 0.07d, 0.1d ]
 //   [0.01d, 0.02d, 0.04d, 0.06d, 0.08d ]
 //          [0.0d, 0.01d, 0.02d, 0.03d, 0.04d, 0.05d, 0.06d, 0.07d, 0.08d, 0.09d, 0.1d]

    List<Double> intersectRatioList = [
           0.5d
     //         0.1d
            //       0.4d,0.5d, 0.6d, 0.7d, 0.8d
     //            0.0d, 0.1d, 0.2d, 0.3d, 0.4d, 0.5d, 0.6d, 0.7d, 0.8d, 0.9d, 1.0d
    ]

    List<QType> queryTypesList = [
            QType.OR_INTERSECT,
           QType.OR1
    ]

    List<LuceneClassifyMethod> classifyMethodList = [
            LuceneClassifyMethod.KNN,
            //     LuceneClassifyMethod.NB
    ]

    ClusterMainECJ() {

        final Date startRun = new Date()

        File timingFile = new File("results/timing.csv")

        if (!timingFile.exists()) {
            timingFile << 'index, queryType, setK, GAtime, KNNtime, overallTime \n'
        }

         //   [false].each { ga_to_set_k ->
   //       [true].each { ga_to_set_k ->  //false to allow GA to know predefined number of clusters
        [true, false].each { ga_to_set_k ->

            GA_TO_SETK = ga_to_set_k
            String parameterFilePath = GA_TO_SETK ? 'src/cfg/clusterGA_K.params' : 'src/cfg/clusterGA.params'

            queryTypesList.each { qType ->
                ClusterQueryECJ.QUERY_TYPE = qType
                println "Query type: $qType"

                intersectRatioList.each { Double minIntersectRatio ->
                    indexList.each { IndexEnum indexEnum ->

                        println "Index Enum: $indexEnum"
                        Indexes.setIndex(indexEnum, minIntersectRatio)
                      //  Indexes.setTermQueryLists(minIntersectRatio)

                        kPenalty.each { kPenalty ->
                            Indexes.K_PENALTY = kPenalty

                            NUMBER_OF_JOBS.times { job ->
                                List<Result> resultList = []
                                List<Result> queryOnlyResultList = []

                                MAX_FIT_JOBS.times { maxFitJob ->

                                    final Date indexTime = new Date()
                                    ParameterDatabase parameters = new ParameterDatabase(new File(parameterFilePath))
                                    EvolutionState state = new EvolutionState()

                                    state = initialize(parameters, job)
                                    if (NUMBER_OF_JOBS >= 1) {
                                        final String jobFilePrefix = "job." + job;
                                        state.output.setFilePrefix(jobFilePrefix);
                                        state.checkpointPrefix = jobFilePrefix + state.checkpointPrefix;
                                    }
                                    //  state.parameters.set(new Parameter("generations"), "7")
                                    state.output.systemMessage("Job: " + job);
                                    state.job = new Object[1]
                                    state.job[0] = new Integer(job)

                                    state.run(EvolutionState.C_STARTED_FRESH);
                                    int popSize = 0;
                                    ClusterFitnessECJ bestClusterFitness = (ClusterFitnessECJ) state.population.subpops.collect { sbp ->
                                        popSize = popSize + sbp.individuals.size()
                                        sbp.individuals.max() { ind ->
                                            ind.fitness.fitness()
                                        }.fitness
                                    }.max { it.fitness() }
                                    final double ecjFitness = bestClusterFitness.fitness;

                                    final int numberOfSubpops = state.parameters.getInt(new Parameter("pop.subpops"), new Parameter("pop.subpops"))
                                    final int wordListSizePop0 = state.parameters.getInt(new Parameter("pop.subpop.0.species.max-gene"), new Parameter("pop.subpop.0.species.max-gene"))
                                    final int genomeSizePop0 = state.parameters.getInt(new Parameter("pop.subpop.0.species.genome-size"), new Parameter("pop.subpop.0.species.genome-size"))
                                    println "wordListSizePop0: $wordListSizePop0 genomeSizePop0 $genomeSizePop0  subPops $numberOfSubpops"

                                    final Date GATime = new Date()
                                    TimeDuration durationGA = TimeCategory.minus(new Date(), indexTime)

                                    Query[] queryArray = bestClusterFitness.queryMap.keySet().toArray() as Query[]
                                    Map<Query, Integer> queryMap = bestClusterFitness.queryMap
                                    BooleanQuery.Builder[] arrayOfQueryBuilders = bestClusterFitness.arrayOfQueryBuilders

                                    Tuple4<Map<Query, Integer>, Integer, Integer, Query[]> t4_qMap_uniqueHitCount_TotalHitCountAllQ_DistinctQueryArray = QuerysetFeatures.getQuerysetFeatures(arrayOfQueryBuilders)

                                    Classify classify = new Classify(queryArray, t4_qMap_uniqueHitCount_TotalHitCountAllQ_DistinctQueryArray.v4)
                                    classify.updateAssignedField(useNonIntersectingClustersForTraining)

                                    classifyMethodList.each { classifyMethod ->
                                        Classifier classifier = classify.getClassifier(classifyMethod, k_for_knn)

                                      //  [true, false].each { queryOnly ->
                                        [false].each { queryOnly ->
                                     //       [true].each { queryOnly ->

                                            Effectiveness effectiveness = new Effectiveness(classifier, queryOnly)
                                            Result result = new Result(ga_to_set_k, indexEnum, qType, effectiveness, classifyMethod, ecjFitness, queryOnly, useNonIntersectingClustersForTraining, t4_qMap_uniqueHitCount_TotalHitCountAllQ_DistinctQueryArray.v2, t4_qMap_uniqueHitCount_TotalHitCountAllQ_DistinctQueryArray.v3, kPenalty, minIntersectRatio, k_for_knn, queryMap, popSize, state.generation, job, maxFitJob)
                                            queryOnly ? queryOnlyResultList << result : resultList << result
                                            result.report(new File('results/results.csv'))
                                            result.queryReport(new File('results/queries.txt'))
                                        }
                                    }

                                    cleanup(state);
                                    println "--------END JOB $job  -----------------------------------------------"
                                }
                                Result maxFitResult = resultList.max { it.fitness }
                                Result maxFitResultQueryOnly = queryOnlyResultList.max { it.fitness }
                                maxFitResult.report(new File('results/maxFitResults.csv'))
                                if (maxFitResultQueryOnly)
                                    maxFitResultQueryOnly.report(new File('results/maxFitResults.csv'))
                            }
                        }
                    }
                }
            }
        }

        final Date endRun = new Date()
        TimeDuration duration = TimeCategory.minus(endRun, startRun)
        println "Duration: $duration"
    }

    static main(args) {
        new ClusterMainECJ()
    }
}