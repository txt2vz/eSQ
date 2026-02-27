package cluster;

import classify.EsqClassify;
import classify.LuceneClassifyMethod;
import groovy.time.TimeCategory;
import groovy.time.TimeDuration;
import index.IndexEnum;
import index.Indexes;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.util.Factory;
import org.apache.lucene.search.BooleanQuery;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;

public class JeneticsMain {
    final static boolean USE_NON_INTERSECTING_CLUSTERS_FOR_TRAINING_KNN = true;
    final static int K_FOR_KNN = 11;
    static String gaEngine = "JENETICS.IO";
    static final double K_PENALTY = 0.03d;
    static EsqQueryBuilder esqQueryBuilder;
    static LuceneClassifyMethod classifyMethod = LuceneClassifyMethod.KNN;

    final static int popSize = 200;
    final static int maxGen = 400;
    final static int maxWordListValue = 70;

    final static int maxK = 8;
    final static int minK = 2;
    final static int maxIntersectListSize = 5;
    final static int minGenomeLength = 16;
    final static int maxGenomeLength = 50;
    final static int numberOfJobs = 2;
    final static int numberMaxFitJobs = 4;
    static BuilderMethod builderMethod = BuilderMethod.INTERSECT;

    static List<IndexEnum> indexList = Arrays.asList(
            IndexEnum.CRISIS3,
            IndexEnum.CRISIS4,
            IndexEnum.NG3,
            IndexEnum.NG5,
            IndexEnum.NG6,
            IndexEnum.R4,
            IndexEnum.R5,
            IndexEnum.R6
    );

    static double searchQueryFitness(final Genotype<IntegerGene> gt) {

        final int k = (gt.get(0)).get(0).allele();
        int[] rootArray = ((IntegerChromosome) gt.get(1)).toArray();
        int[] intersectArray = ((IntegerChromosome) gt.get(2)).toArray();

        BooleanQuery.Builder[] bqbArray = esqQueryBuilder.buildQueries(rootArray, intersectArray, k);
        QuerySet querySet = new QuerySet(bqbArray);
        final int uniqueHits = querySet.getTotalHitsReturnedByOnlyOneQuery();
        final double f = uniqueHits * (1.0 - (K_PENALTY * k));

        return f;
    }

    public static void main(String[] args) throws Exception {

        final Date startRun = new Date();
        List<Double> bestMaxFitV = new ArrayList<>();

        for (IndexEnum index : indexList) {
            Indexes.setIndex(index);
            Indexes.setImportantTermQueryList();

            List<Phenotype<IntegerGene, Double>> jeneticsResultList = new ArrayList<>();

            IntStream.range(0, numberOfJobs).forEach(jobNumber -> {
                List<EsqResultDetail> esqResultDetailList = new ArrayList<>();

                IntStream.range(0, numberMaxFitJobs).forEach(maxFitjob -> {

                  //  builderMethod = (maxFitjob % 2 == 0) ? BuilderMethod.INTERSECT : BuilderMethod.SINGLE;  //vary the builder Method  - maxFitJobs will select best based on fitness
                    esqQueryBuilder = new EsqQueryBuilder(Indexes.termQueryList, Indexes.orderedIntersectMap, builderMethod);

                    final Factory<Genotype<IntegerGene>> gtf = Genotype.of(
                            IntegerChromosome.of(minK, maxK, 1),  //possible values of k
                            IntegerChromosome.of(0, maxWordListValue, maxK), //rootword
                            IntegerChromosome.of(-1, maxIntersectListSize, maxIntersectListSize * maxK) //intersect words -1 indicates no word added to query
                    );

                    final Engine<IntegerGene, Double> engine = Engine
                            .builder(
                                    JeneticsMain::searchQueryFitness, gtf)
                            .populationSize(popSize)
                            .selector(new TournamentSelector<>(3))
                            .alterers(
                                    //should be good for single gene chromosome
                                    PartialAlterer.of(new MeanAlterer<IntegerGene, Double>(0.3), 0),
                                    PartialAlterer.of(new GaussianMutator<IntegerGene, Double>(0.4), 0),

                                    PartialAlterer.of(new SinglePointCrossover<IntegerGene, Double>(0.3), 1),
                                    PartialAlterer.of(new SinglePointCrossover<IntegerGene, Double>(0.3), 2),
                                    new Mutator<>(0.1)
                            )
                            .build();

                    final EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();
                    AtomicReference<Double> fitness = new AtomicReference<>((double) 0);

                    final Phenotype<IntegerGene, Double> jeneticsResult =

                            engine.stream()
                                    .limit(maxGen)
                                    .peek(ind -> {
                                        Genotype<IntegerGene> g = ind.bestPhenotype().genotype();
                                        final int k0 = (g.get(0)).get(0).allele();
                                        fitness.set(ind.bestPhenotype().fitness());

                                        if (ind.generation() % 20 == 0) {
                                            System.out.println("Gen: " + ind.generation() + " Index: " + index.name() + " bestPhenoFit: " + ind.bestFitness() + " k: " + k0 );//+ " phenotype: " + ind.bestPhenotype());
                                        }
                                    })
                                    .peek(statistics)
                                    .collect(toBestPhenotype());

                    jeneticsResultList.add(jeneticsResult);
                    Genotype<IntegerGene> g = jeneticsResult.genotype();

                    final int k = (g.get(0)).get(0).allele();
                    int[] intArrayBestOfRun = ((IntegerChromosome) g.get(1)).toArray();
                    int[] interserctArrayBestOfRun = ((IntegerChromosome) g.get(2)).toArray();

                    BooleanQuery.Builder[] arrayOfQueryBuilders = esqQueryBuilder.buildQueries(intArrayBestOfRun, interserctArrayBestOfRun, k);

                    QuerySet querySet = new QuerySet(arrayOfQueryBuilders);
                    EsqClassify esqClassify = new EsqClassify(querySet.getQueryArray(), querySet.getNonIntersectingQueries());
                    esqClassify.updateAssignedClusterField(USE_NON_INTERSECTING_CLUSTERS_FOR_TRAINING_KNN);  //update the field in the index with result of classification (KNN)

                    ExpandQueryDefinedClusters expandQueryDefinedClusters = new ExpandQueryDefinedClusters(esqClassify.getClassifier(classifyMethod, K_FOR_KNN));

                    EsqResultDetail esqResultDetail = new EsqResultDetail(index, expandQueryDefinedClusters, jeneticsResult.fitness(), querySet, classifyMethod, builderMethod, true, USE_NON_INTERSECTING_CLUSTERS_FOR_TRAINING_KNN, K_PENALTY, QueryTermIntersectRatio.getMIN_INTERSECT_RATIO(), K_FOR_KNN, popSize, (int) jeneticsResult.generation(), jobNumber, maxFitjob, gaEngine);
                    esqResultDetail.report(new File("results//resultsJenetics.csv"));
                    esqResultDetail.queryReport(new File("results//jeneticsQueries.txt"));
                    esqResultDetailList.add(esqResultDetail);

                    System.out.println("Gen: " + jeneticsResult.generation() + " Fitness: " + jeneticsResult.fitness() + " v: " + expandQueryDefinedClusters.getvMeasure());
                    System.out.println("*********************************************************** \n");
                });

                Optional<EsqResultDetail> maxResultForJob = esqResultDetailList.stream().max(Comparator.comparing(EsqResultDetail::getFitness));
                maxResultForJob.get().report(new File("results//maxFitResultsJenetics.csv"));
                bestMaxFitV.add(maxResultForJob.get().getV());
            });
        }

        final double average = bestMaxFitV.stream()
                .collect(Collectors.averagingDouble(Double::doubleValue));

        System.out.println("Average maxFit v : " + average + " List of v " + bestMaxFitV);

        final Date endRun = new Date();
        TimeDuration duration = TimeCategory.minus(endRun, startRun);
        System.out.println("Duration: " + duration);
    }
}