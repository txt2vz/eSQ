package cluster;

import groovy.time.TimeCategory;
import groovy.time.TimeDuration;
import index.IndexEnum;
import index.Indexes;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.util.Factory;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;
import io.jenetics.util.IntRange;

import org.apache.lucene.search.BooleanQuery;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;

public class JeneticsMain {

    static String gaEngine = "JENETICS.IO";
    static final double K_PENALTY = 0.03d;
    static EsqQueryBuilder esqQueryBuilder;
    static EsqQueryBuilderMethod eSqQueryBuilderMethod = EsqQueryBuilderMethod.INTERSECT;

    final static int popSize = 100;
    final static int maxGen = 1200;
    final static int maxWordListValue = 80;
    final static int maxK = 8;
    final static int minK = 2;
    final static int maxIntersectListSize = 2;
    final static int minGenomeLength = 16;
    final static int maxGenomeLength = 50;
    final static int numberOfJobs = 2;
    final static int numberMaxFitJobs = 5;
    final static boolean expandKeywordClustersWithPython = true;

    static List<IndexEnum> indexList = Arrays.asList(
           IndexEnum.CRISIS3,
           IndexEnum.CRISIS4,
            IndexEnum.CRISIS6,
            IndexEnum.NG3,
            IndexEnum.NG5,
            IndexEnum.NG6,
            IndexEnum.R4,
            IndexEnum.R5,
            IndexEnum.R6          
         );

    static double searchQueryFitness(final Genotype<IntegerGene> gt) {

        final int k = (gt.get(0)).get(0).allele();
        BooleanQuery.Builder[] bqbArray;
        try {
            bqbArray = esqQueryBuilder.buildQueries(gt, k);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        QuerySet querySet = new QuerySet(bqbArray);

        final int uniqueHits = querySet.getTotalHitsReturnedByOnlyOneQuery();
        return uniqueHits * (1.0 - (K_PENALTY * k));
    }

    public static void main(String[] args) throws Exception {

        final Date startRun = new Date();
    
        File keywordDir = new File("Keywords_JSON");
        try {
            // delete existing keyword directory and contents if it exists to avoid
            // confusion with old results
            FileUtils.deleteDirectory(keywordDir);
            System.out.println("Keyword directory deleted successfully!");
        } catch (IOException e) {
            System.out.println("Failed to delete keyword directory: " + e.getMessage());
        }
        
        for (IndexEnum index : indexList) {
            Indexes.setIndex(index);
            Indexes.setImportantTermQueryList(maxWordListValue);

            List<Phenotype<IntegerGene, Double>> jeneticsResultList = new ArrayList<>();

            IntStream.range(0, numberOfJobs).forEach(jobNumber -> {

                double[] bestJobFitness = { Double.NEGATIVE_INFINITY };
                QuerySet[] bestJobQuerySet = new QuerySet[1];

                IntStream.range(0, numberMaxFitJobs).forEach(maxFitjob -> {

                    
                    esqQueryBuilder = new EsqQueryBuilder(Indexes.termQueryList, Indexes.orderedIntersectMap,
                            eSqQueryBuilderMethod);

                    final Factory<Genotype<IntegerGene>> gtf = Genotype.of(
                            IntegerChromosome.of(minK, maxK, 1), // possible values of k
                            IntegerChromosome.of(0, maxWordListValue, maxK), // rootword
                            
                            //intersect word -1 indicates no word to be added to the query
                            IntegerChromosome.of(-1, maxIntersectListSize, maxIntersectListSize * maxK)                   
                    );

                    final Engine<IntegerGene, Double> engine = Engine
                            .builder(
                                    JeneticsMain::searchQueryFitness, gtf)
                            .populationSize(popSize)
                            .selector(new TournamentSelector<>(3))
                            .alterers(
                                    PartialAlterer.of(new MeanAlterer<IntegerGene, Double>(0.1), 0),                             
                                    PartialAlterer.of(new GaussianMutator<IntegerGene, Double>(0.3), 0),
                                    PartialAlterer.of(new SinglePointCrossover<IntegerGene, Double>(0.3), 1),
                                    PartialAlterer.of(new SinglePointCrossover<IntegerGene, Double>(0.3), 2),
                                    new Mutator<>(0.1))
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

                                        if (ind.generation() % 90 == 0) {
                                            System.out.println("Gen: " + ind.generation() + " Index: " + index.name()
                                                    + " bestPhenoFit: " + ind.bestFitness() + " k: " + k0);                                               
                                        }
                                    })
                                    .peek(statistics)
                                    .collect(toBestPhenotype());

                    jeneticsResultList.add(jeneticsResult);
                    Genotype<IntegerGene> genotype = jeneticsResult.genotype();

                    final int k = (genotype.get(0)).get(0).allele();
                    BooleanQuery.Builder[] arrayOfQueryBuilders;
                    try {
                        arrayOfQueryBuilders = esqQueryBuilder.buildQueries(genotype, k);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    QuerySet querySet = new QuerySet(arrayOfQueryBuilders);
                    querySet.printQueryMap();

                    if (jeneticsResult.fitness() > bestJobFitness[0]) {
                        bestJobFitness[0] = jeneticsResult.fitness();
                        bestJobQuerySet[0] = querySet;
                    }

                    System.out.println("Gen: " + jeneticsResult.generation() + " Fitness: " + jeneticsResult.fitness());
                    System.out.println("*********************************************************** \n");
                });

                System.out.println("Best fitness for jobNumber " + jobNumber + ": " + bestJobFitness[0]);
                if (bestJobQuerySet[0] != null) {
                    bestJobQuerySet[0].printQueryMap();
                    bestJobQuerySet[0].printQueryTermLists();

                    if (!keywordDir.exists()) {
                        keywordDir.mkdirs();
                    }
                    File keywordFile = new File(keywordDir, index.name() + "_keywordSet_" + jobNumber + ".json");
                    try {
                        bestJobQuerySet[0].writeQueryTermsJson(keywordFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Best query keyword sets written to " + keywordFile.getAbsolutePath());
                } else {
                    System.out.println("No best query map was selected for jobNumber " + jobNumber);
                }
            });
        }

        final Date endJavaRun = new Date();
        TimeDuration duration = TimeCategory.minus(endJavaRun, startRun);
        System.out.println("Duration for Keyword Generation: " + duration);

        if (expandKeywordClustersWithPython) {
            int pythonExitCode = CallPythonToExpandKeywordClusters.processPythonExpandClusters();
            if (pythonExitCode == 0) {
                System.out.println("Python expansion of keyword clusters completed successfully.");
                final Date endTotalRun = new Date();
                TimeDuration totalDuration = TimeCategory.minus(endTotalRun, startRun);
                System.out.println("Total Duration for Keyword Generation and Expansion: " + totalDuration);
            } else {
                System.out.println("Python expansion of keyword clusters did not complete successfully.");
            }
        }
    }
}