package cluster;

import classify.Classify;
import classify.LuceneClassifyMethod;
import groovy.time.TimeCategory;
import groovy.time.TimeDuration;
import index.IndexEnum;
import index.Indexes;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.util.Factory;
import io.jenetics.util.IntRange;
import org.apache.lucene.search.BooleanQuery;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;

public class JeneticsMain {
    final static boolean USE_NON_INTERSECTING_CLUSTERS_FOR_TRAINING_KNN = true;
    final static int K_FOR_KNN = 10;
    static String gaEngine = "JENETICS.IO";
    static final double K_PENALTY = 0.03d;
    static EsqQueryBuilder esqQueryBuilder;

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

        int[] intArray = ((IntegerChromosome) gt.get(0)).toArray();
        final int k = (gt.get(1)).get(0).allele();
        BooleanQuery.Builder[] bqbArray = esqQueryBuilder.buildQueries(intArray, k);
        QuerySet querySet = new QuerySet(bqbArray);
        final int uniqueHits = querySet.getTotalHitsReturnedByOnlyOneQuery();
        final double f = uniqueHits * (1.0 - (K_PENALTY * k));
        assert f > 0;
        return f;
    }

    public static void main(String[] args) throws Exception {

        final Date startRun = new Date();
        final int popSize = 60;
        final int maxGen = 2000;
        final int maxWordListValue = 60;
        final LuceneClassifyMethod classifyMethod = LuceneClassifyMethod.KNN;
        final int minGenomeLength = 16;
        final int maxGenomeLength = 40;
        final int numberOfJobs = 2;
        final int numberMaxFitJobs = 8;
        final BuilderMethod builderMethod = BuilderMethod.BLOCKS;
        List<Double> bestMaxFitV = new ArrayList<>();


        for (IndexEnum index : indexList) {
            Indexes.setIndex(index);
            esqQueryBuilder = new EsqQueryBuilder(Indexes.termQueryList, builderMethod);
            List<Phenotype<IntegerGene, Double>> jeneticsResultList = new ArrayList<>();

            IntStream.range(0, numberOfJobs).forEach(jobNumber -> {
                List<EsqResultDetail> esqResultDetailList = new ArrayList<>();

                IntStream.range(0, numberMaxFitJobs).forEach(maxFitjob -> {

                    final Factory<Genotype<IntegerGene>> gtf = Genotype.of(
                            IntegerChromosome.of(0, maxWordListValue, IntRange.of(minGenomeLength, maxGenomeLength)),
                            IntegerChromosome.of(2, 9, 1)  //possible values of k
                    );

                    final Engine<IntegerGene, Double> engine = Engine
                            .builder(
                                    JeneticsMain::searchQueryFitness, gtf)
                            .populationSize(popSize)
                            .selector(new TournamentSelector<>(3))
                            .alterers(
                                    PartialAlterer.of(new SinglePointCrossover<IntegerGene, Double>(0.3), 0),
                                    //   PartialAlterer.of(new MultiPointCrossover<IntegerGene, Double>(0.3), 0),
                                    PartialAlterer.of(new GaussianMutator<IntegerGene, Double>(0.4), 1),
                                    new Mutator<>(0.1)
                            )
                            .build();

                    final EvolutionStatistics<Double, ?>
                            statistics = EvolutionStatistics.ofNumber();
                    AtomicReference<Double> fitness = new AtomicReference<>((double) 0);

                    final Phenotype<IntegerGene, Double> jeneticsResult =

                            engine.stream()
                                    .limit(maxGen)
                                    .peek(ind -> {
                                        Genotype<IntegerGene> g = ind.bestPhenotype().genotype();
                                        final int k0 = (g.get(1)).get(0).allele();
                                        fitness.set(ind.bestPhenotype().fitness());

                                        if (ind.generation() % 20 == 0) {
                                            System.out.println("Gen: " + ind.generation() + " Index: " + index.name() + " bestPhenoFit: " + ind.bestFitness() + " k: " + k0);
                                        }
                                    })
                                    .peek(statistics)
                                    .collect(toBestPhenotype());

                    jeneticsResultList.add(jeneticsResult);
                    Genotype<IntegerGene> g = jeneticsResult.genotype();

                    int[] intArrayBestOfRun = ((IntegerChromosome) g.get(0)).toArray();
                    final int k = (g.get(1)).get(0).allele();

                    BooleanQuery.Builder[] arrayOfQueryBuilders = esqQueryBuilder.buildQueries(intArrayBestOfRun, k);

                    QuerySet querySet = new QuerySet(arrayOfQueryBuilders);
                    Classify classify = new Classify(querySet.getQueryArray(), querySet.getNonIntersectingQueries());
                    classify.updateAssignedField(USE_NON_INTERSECTING_CLUSTERS_FOR_TRAINING_KNN);

                    Effectiveness effectiveness = new Effectiveness(classify.getClassifier(classifyMethod, K_FOR_KNN));
                    EsqResultDetail esqResultDetail = new EsqResultDetail(index, effectiveness, jeneticsResult.fitness(), querySet, classifyMethod, false, USE_NON_INTERSECTING_CLUSTERS_FOR_TRAINING_KNN, K_PENALTY, QueryTermIntersect.getMIN_INTERSECT_RATIO(), K_FOR_KNN, popSize, (int) jeneticsResult.generation(), jobNumber, maxFitjob, gaEngine);
                    esqResultDetail.report(new File("results//resultsJenetics.csv"));
                    esqResultDetail.queryReport(new File("results//jeneticsQueries.txt"));
                    esqResultDetailList.add(esqResultDetail);

                    System.out.println("Gen: " + jeneticsResult.generation() + " Fitness: " + jeneticsResult.fitness() + " v: " + effectiveness.getvMeasure());
                    System.out.println("*********************************************************** \n");
                });

                Optional<EsqResultDetail> maxResultForJob = esqResultDetailList.stream().max(Comparator.comparing(EsqResultDetail::getFitness));
                maxResultForJob.get().report(new File("results//maxFitResultsJenetics.csv"));
                bestMaxFitV.add(maxResultForJob.get().getV());

            });
        };

        double average = bestMaxFitV.stream()
                .collect(Collectors.averagingDouble(Double::doubleValue));

        System.out.println("Average maxFit v : " + average + " List of v " + bestMaxFitV);

        final Date endRun = new Date();
        TimeDuration duration = TimeCategory.minus(endRun, startRun);
        System.out.println("Duration: " + duration);
    }
}