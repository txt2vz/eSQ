package cluster;

import classify.Classify;
import classify.LuceneClassifyMethod;
import groovy.time.TimeCategory;
import groovy.time.TimeDuration;
import index.*;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.util.IntRange;
import org.apache.lucene.classification.Classifier;
import org.apache.lucene.search.BooleanQuery;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.util.Factory;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import index.IndexEnum;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;

public class JeneticsMain {
    final static boolean useNonIntersectingClustersForTrainingKNN = true;
    final static int k_for_knn = 10;
    final static QType qType = QType.OR_INTERSECT;// QType.OR1;
    static IndexEnum indexEnum;
    final static boolean GA_TO_SETK = true;
    static String gaEngine = "JENETICS.IO";
    static final double kPenalty = 0.03d;
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
        final int k = getK(gt, indexEnum, GA_TO_SETK);
        int[] intArray = ((IntegerChromosome) gt.get(0)).toArray();
        BooleanQuery.Builder[] bqbArray = QueryBuilders.getMultiWordQuery(intArray, Indexes.termQueryList, k);
        QuerySet querySet = new QuerySet(bqbArray);

        final int uniqueHits = querySet.getTotalHitsReturnedByOnlyOneQuery();
       // final int overlappingHits = querySet.getTotalHitsAllQueries() - uniqueHits;
        final double f = (GA_TO_SETK) ? uniqueHits * (1.0 - (kPenalty * k)) : uniqueHits;
     //   final double f = (GA_TO_SETK) ? (uniqueHits - overlappingHits) * (1.0 - (kPenalty * k)) : uniqueHits;
        //final double f = uniqueHits - overlappingHits;

        return (f > 0) ? f : 0.0d;
    }

    public static void main(String[] args) throws Exception {

        final Date startRun = new Date();
        final int popSize = 120;
        final int maxGen = 800;
        final int maxWordListValue = 80;
        final LuceneClassifyMethod classifyMethod = LuceneClassifyMethod.KNN;
        final int minGenomeLength = 16;
        final int maxGenomeLength = 40;
        final int numberOfJobs = 2;
        final int numberMaxFitJobs = 3;
        List<Double> bestMaxFitv = new ArrayList<>();

        indexList.stream().forEach(index -> {
            Indexes.setIndex(index);
            List<Phenotype<IntegerGene, Double>> resultList = new ArrayList<>();
            indexEnum = index;

            IntStream.range(0, numberOfJobs).forEach(jobNumber -> {
                List<Result> resultListForJob = new ArrayList<>();

                IntStream.range(0, numberMaxFitJobs).forEach(maxFitjob -> {

                    final Factory<Genotype<IntegerGene>> gtf =

                            (GA_TO_SETK) ?
                                    Genotype.of(
                                            IntegerChromosome.of(0, maxWordListValue, IntRange.of(minGenomeLength, maxGenomeLength)),
                                            IntegerChromosome.of(2, 9, 1)) :  //psossible values for k

                                    Genotype.of(
                                            IntegerChromosome.of(0, maxWordListValue, IntRange.of(minGenomeLength, maxGenomeLength))
                                    );

                    final Engine<IntegerGene, Double> engine = Engine.
                            builder(
                                    JeneticsMain::searchQueryFitness, gtf)
                            .populationSize(popSize)

                            .survivorsSelector(new TournamentSelector<>(3))
                            //  .survivorsSelector(new EliteSelector<>(1))
                            .offspringSelector(new TournamentSelector<>(3))

                            //    .alterers(
                            //         new Mutator<>(0.03) ,
                            //       new LineCrossover<>(0.2))
                            //    .survivorsSelector(new TournamentSelector<>(5)).survivorsSelector(new EliteSelector<>(2))
                            .alterers(new Mutator<>(0.2), new SinglePointCrossover<>(0.3))
                            //  .alterers(new Mutator<>(0.1), new LineCrossover<>(0.3))
                            //  new MeanAlterer <>(0.6))
                            //    .alterers(new Mutator<>(0.3), new MultiPointCrossover<>(0.5))
                            .build();

                    final EvolutionStatistics<Double, ?>
                            statistics = EvolutionStatistics.ofNumber();
                    AtomicReference<Double> fitness = new AtomicReference<>((double) 0);

                    final Phenotype<IntegerGene, Double> result =

                            engine.stream()
                                    .limit(maxGen)
                                    .peek(ind -> {
                                        Genotype<IntegerGene> g = ind.bestPhenotype().genotype();
                                        final int k0 = getK(g, index, GA_TO_SETK);

                                        fitness.set(ind.bestPhenotype().fitness());

                                      //  searchQueryFitness(g);

                                        System.out.println("Gen: " + ind.generation() + " Index: " + index.name() + " bestPhenoFit " + ind.bestFitness() + " worst "  + ind.worstFitness() + " k " + k0);   //+ //" uniqueHits: " + queryDataGen.getV2() + " querySet F1: " + queryDataGen.getV4());
                                    })
                                    .peek(statistics)
                                    .collect(toBestPhenotype());

                    resultList.add(result);
                    Genotype<IntegerGene> g = result.genotype();

                    int[] intArrayBestOfRun = ((IntegerChromosome) g.get(0)).toArray();
                    final int k = getK(g, index, GA_TO_SETK);

                    BooleanQuery.Builder[] arrayOfQueryBuilders = QueryBuilders.getMultiWordQuery(intArrayBestOfRun, Indexes.termQueryList, k);

                    QuerySet querySet = new QuerySet(arrayOfQueryBuilders);

                    Classify classify = new Classify(querySet.getQueryArray(), querySet.getNonIntersectingQueries());

                    classify.updateAssignedField(useNonIntersectingClustersForTrainingKNN);
                    Classifier classifier = classify.getClassifier(classifyMethod, k_for_knn);

                    Effectiveness effectiveness = new Effectiveness(classifier, false);

                    System.out.println("Result:  " + result + " Gen: " + result.generation() + " v: " + effectiveness.getvMeasure());

                    Result results = new Result(GA_TO_SETK, indexEnum, qType, effectiveness, result.fitness(), querySet, classifyMethod, false, useNonIntersectingClustersForTrainingKNN, kPenalty, QueryTermIntersect.getMIN_INTERSECT_RATIO(), k_for_knn, popSize, (int) result.generation(), jobNumber, maxFitjob, gaEngine);

                    results.report(new File("results//resultsJenetics.csv"));
                    results.queryReport(new File("results//jeneticsQueries.txt"));
                    resultListForJob.add(results);
                });

                Optional<Result> maxResultForJob = resultListForJob.stream().max(Comparator.comparing(Result::getFitness));
           //     System.out.println("max r fit " + maxResultForJob.get().getFitness());
                maxResultForJob.get().report(new File("results//maxFitResultsJenetics.csv"));
      //          System.out.println("Av v: " +    maxResultForJob.get().getAverageV()  );
                bestMaxFitv.add(maxResultForJob.get().getV());

            });
        });


        double average = bestMaxFitv.stream()
                .collect(Collectors.averagingDouble(Double::doubleValue));

        System.out.println("Average v : " + average + " list of v " + bestMaxFitv);



        final Date endRun = new Date();
        TimeDuration duration = TimeCategory.minus(endRun, startRun);
        System.out.println("Duration: " + duration);
    }

    static int getK(Genotype g, IndexEnum indexEnum, final boolean setk) {

        if (!setk) return indexEnum.getNumberOfCategories();

        final int k = ((IntegerChromosome) g.get(1)).get(0).allele();
        return k;

        // return (setk) ? ((IntegerChromosome) g.get(1)).get(0).allele() : indexEnum.getNumberOfCategories();
    }
}