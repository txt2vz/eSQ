package cluster

import classify.ClassifyUnassigned
import classify.LuceneClassifyMethod
import classify.UpdateAssignedFieldInIndex
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

    final static int NUMBER_OF_JOBS = 4
    final static int MAX_FIT_JOBS = 3
    final static boolean onlyDocsInOneCluster = false
    final static boolean luceneClassify = true
    final static boolean useSameIndexForEffectivenessMeasure = true
    final static String gaEngine = "ECJ";
    static boolean SETK

    //indexes suitable for clustering.
    List<Tuple2<IndexEnum, IndexEnum>> clusteringIndexes = [
//
            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.R4, IndexEnum.R4TEST),
            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.R5, IndexEnum.R5TEST),
            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.R6, IndexEnum.R6TEST),

            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.NG3, IndexEnum.NG3TEST),
            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.NG5, IndexEnum.NG5TEST),
            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.NG6, IndexEnum.NG6TEST),

            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.CLASSIC4, IndexEnum.CLASSIC4TEST),

            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.CRISIS3, IndexEnum.CRISIS3TEST)
    ]

    List<Double> kPenalty = [0.03d]
        //   [0.0d, 0.01d, 0.02d, 0.03d, 0.04d, 0.05d, 0.06d, 0.07d, 0.08d, 0.09d, 0.1d]

    List<Double> intersectRatioList = [
            0.6d
        //     0.0d, 0.1d, 0.2d, 0.3d, 0.4d, 0.5d, 0.6d, 0.7d, 0.8d, 0.9d, 1.0d
    ]

    List<QType> queryTypesList = [
               QType.OR_INTERSECT,
                QType.OR1
            //       QType.AND_INTERSECT
    ]

    List<LuceneClassifyMethod> classifyMethodList = [
            LuceneClassifyMethod.KNN,
            //          LuceneClassifyMethod.NB
    ]

    ClusterMainECJ() {

        final Date startRun = new Date()
        Reports reports = new Reports();

        File timingFile = new File("results/timing.csv")

        if (!timingFile.exists()) {
            timingFile << 'index, queryType, setK, GAtime, KNNtime, overallTime \n'
        }

    // //      [false].each { set_k ->
     ///    [true].each { set_k ->  //false to allow GA to know predefined number of clusters
        [true, false].each { set_k ->

            SETK = set_k
            String parameterFilePath = SETK ? 'src/cfg/clusterGA_K.params' : 'src/cfg/clusterGA.params'
            // 'src/cfg/clusterGA_Kmap.params'

            queryTypesList.each { qType ->
                ClusterQueryECJ.QUERY_TYPE = qType
                println "Query type $qType"

                intersectRatioList.each { Double minIntersectRatio ->

                    clusteringIndexes.each { Tuple2<IndexEnum, IndexEnum> trainTestIndexes ->

                        println "Index Enum trainTestIndexes: $trainTestIndexes"
                        Indexes.setIndex(trainTestIndexes.v1)
                        Indexes.setTermQueryLists(minIntersectRatio)

                        kPenalty.each { kPenalty ->
                            Indexes.K_PENALTY = kPenalty

                            NUMBER_OF_JOBS.times { job ->

                                MAX_FIT_JOBS.times { maxFit ->

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
                                    timingFile << trainTestIndexes.v1.name() + ",  " + qType + ",  " + set_k + ", " + durationGA.toMilliseconds()

                                    Set<Query> queries = bestClusterFitness.queryMap.keySet().asImmutable()
                                    List<BooleanQuery.Builder> bqbList = bestClusterFitness.bqbList

                                    Tuple6<Map<Query, Integer>, Integer, Integer, Double, Double, Double> t6QuerySetResult = QuerySet.querySetInfo(bqbList)

                                    UpdateAssignedFieldInIndex.updateAssignedField(trainTestIndexes.v1, queries, onlyDocsInOneCluster)

                                    classifyMethodList.each { classifyMethod ->
                                        Classifier classifier = ClassifyUnassigned.getClassifierForUnassignedDocuments(trainTestIndexes.first, classifyMethod)

                                        TimeDuration durationKNN = TimeCategory.minus(new Date(), GATime)
                                        TimeDuration overallTime = TimeCategory.minus(new Date(), indexTime)
                                        timingFile << ",  " + durationKNN.toMilliseconds() + ', ' + overallTime.toMilliseconds() + '\n'
                                        IndexEnum checkEffectifnessIndex = useSameIndexForEffectivenessMeasure ? trainTestIndexes.v1 : trainTestIndexes.v2
                                        Tuple3 t3ClassiferResult = Effectiveness.classifierEffectiveness(classifier, checkEffectifnessIndex, bestClusterFitness.k)

                                        reports.reports(trainTestIndexes.v1, t6QuerySetResult.v1, t6QuerySetResult.v2, t6QuerySetResult.v3, t6QuerySetResult.v4, t6QuerySetResult.v5, t6QuerySetResult.v6, t3ClassiferResult.v1, t3ClassiferResult.v2, t3ClassiferResult.v3, ecjFitness, qType, SETK, classifyMethod, minIntersectRatio, kPenalty, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, state.generation, gaEngine, job, maxFit)
                                    }
                                    cleanup(state);
                                    println "--------END JOB $job  -----------------------------------------------"
                                }
                                reports.reportMaxFitness()
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