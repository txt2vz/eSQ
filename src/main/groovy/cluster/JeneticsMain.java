package cluster;

import classify.ClassifyUnassigned;
import classify.LuceneClassifyMethod;
import classify.UpdateAssignedFieldInIndex;
import groovy.lang.Tuple3;
import groovy.lang.Tuple6;
import groovy.time.TimeCategory;
import groovy.time.TimeDuration;
import index.*;
import io.jenetics.engine.EvolutionStatistics;
import org.apache.lucene.classification.Classifier;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.util.Factory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import index.IndexEnum;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;


public class JeneticsMain {

    static List<TermQuery> termQueryList;
    static QType qType = //QType.OR1;
          QType.OR_INTERSECT;
    static IndexEnum indexEnum;
    static IndexReader ir;
    final static boolean SETK = true;
    static String gaEngine = "JENETICS.IO";
    static final double kPenalty = 0.03d;

    //static int k;
    static List<IndexEnum> ieList = Arrays.asList(
            IndexEnum.CRISIS3,
            IndexEnum.CLASSIC4,
            IndexEnum.NG3,
            IndexEnum.NG5,
            IndexEnum.NG6,
            IndexEnum.R4,
            IndexEnum.R5,
            IndexEnum.R6
    );

    static double searchQueryFitness(final Genotype<IntegerGene> gt) {
        final int k = getK(gt, indexEnum, SETK);
        int[] intArray = ((IntegerChromosome) gt.get(0)).toArray();

        //  List<BooleanQuery.Builder> bqbList = QuerySet.getQueryBuilderList(intArray, termQueryList, k, qType);
        List<BooleanQuery.Builder> bqbList = QuerySet.getQueryBuilderList(intArray, k, qType);
        final int uniqueHits = UniqueHits.getUniqueHits(bqbList).getV2();

        final double f = (SETK) ? uniqueHits * (1.0 - (kPenalty * k)) : uniqueHits;
        return (f > 0) ? f : 0.0d;
    }

    public static void main(String[] args) throws Exception {

        final Date startRun = new Date();
        final int popSize = 512;
        final int maxGen = 120;
        final int maxGene = 80;
        final LuceneClassifyMethod classifyMethod = LuceneClassifyMethod.KNN;
        final int setkMaxNumberOfCategories = 9;

        final int numberOfJobs = 4;
        final int numberMaxFitJobs = 5;
        final int numberOfSubPops = 1;

        final int maxGenomeLength = 19;
        final boolean onlyDocsInOneClusterForClassifier = false;
        final double minIntersectRatio = 0.6d;
        Reports reports = new Reports();

        ieList.stream().forEach(ie -> {
            Indexes.setIndex(ie, true);
            Indexes.setTermQueryLists(minIntersectRatio);
            List<Phenotype<IntegerGene, Double>> resultList = new ArrayList<Phenotype<IntegerGene, Double>>();
            indexEnum = ie;
            termQueryList = Indexes.getTermQueryList();

            IntStream.range(0, numberOfJobs).forEach(jobNumber -> {
                IntStream.range(0, numberMaxFitJobs).forEach(maxFitjob -> {

                    final int maxCats = (SETK) ? setkMaxNumberOfCategories : indexEnum.getNumberOfCategories();
                    final int genomeLength = (qType == QType.OR1) ? maxCats : maxGenomeLength;

                    final Factory<Genotype<IntegerGene>> gtf =
                            (SETK) ?
                                    Genotype.of(
                                            IntegerChromosome.of(0, maxGene, genomeLength),
                                            IntegerChromosome.of(2, setkMaxNumberOfCategories)) :  //possible values for k

                                    Genotype.of(
                                            IntegerChromosome.of(0, maxGene, genomeLength));
                    //  IntegerChromosome.of(0, 100, IntRange.of(2, 8)));//,

                    final Engine<IntegerGene, Double> engine = Engine.
                            builder(
                                    JeneticsMain::searchQueryFitness,
                                    gtf)
                            .populationSize(popSize)
                            // .survivorsSelector(new
                            // StochasticUniversalSelector<>()).offspringSelector(new
                            // TournamentSelector<>(5))

                            .survivorsSelector(new TournamentSelector<>(5))
                            .survivorsSelector(new EliteSelector<>(2))
                            .offspringSelector(new TournamentSelector<>(5))
                            .alterers(new Mutator<>(0.2), //new MultiPointCrossover<>())
                                    new SinglePointCrossover<>(0.7))
                            .build();

                    final EvolutionStatistics<Double, ?>
                            statistics = EvolutionStatistics.ofNumber();
                    AtomicReference<Double> fitness = new AtomicReference<>((double) 0);

                    final Phenotype<IntegerGene, Double> result =

                            engine.stream()
                                    .limit(maxGen)
                                    .peek(ind -> {
                                        Genotype<IntegerGene> g = ind.bestPhenotype().genotype();
                                        int[] intArrayBestGen = ((IntegerChromosome) g.get(0)).toArray();
                                        final int k = getK(g, ie, SETK);

                                        fitness.set(ind.bestPhenotype().fitness());

                                        // List<BooleanQuery.Builder> bqbList = QuerySet.getQueryBuilderList(intArrayBestGen, termQueryList, k, qType);
                                        List<BooleanQuery.Builder> bqbList = QuerySet.getQueryBuilderList(intArrayBestGen, k, qType);
                                        Tuple6<Map<Query, Integer>, Integer, Integer, Double, Double, Double> queryDataGen = QuerySet.querySetInfo(bqbList, true);
                                        System.out.println("Gen: " + ind.generation() + " bestPhenoFit " + ind.bestPhenotype().fitness() + " fitness: " + ind.bestFitness() + " uniqueHits: " + queryDataGen.getV2() + " querySet F1: " + queryDataGen.getV4());
                                        System.out.println();

                                    })
                                    .peek(statistics)
                                    .collect(toBestPhenotype());

                    System.out.println("Final result  " + result);
                    resultList.add(result);
                    Genotype<IntegerGene> g = result.genotype();

                    int[] intArrayBestOfRun = ((IntegerChromosome) g.get(0)).toArray();
                    final int k = getK(g, ie, SETK);

                    // List<BooleanQuery.Builder> bqbList = QuerySet.getQueryBuilderList(intArrayBestOfRun, termQueryList, k, qType);
                    List<BooleanQuery.Builder> bqbList = QuerySet.getQueryBuilderList(intArrayBestOfRun, k, qType);
                    Tuple6<Map<Query, Integer>, Integer, Integer, Double, Double, Double> t6QuerySetResult = QuerySet.querySetInfo(bqbList);

                    Classifier classifier = ClassifyUnassigned.getClassifierForUnassignedDocuments(ie, LuceneClassifyMethod.KNN);

                    UpdateAssignedFieldInIndex.updateAssignedField(ie, t6QuerySetResult.getV1().keySet(), onlyDocsInOneClusterForClassifier);

                    Tuple3<Double, Double, Double> t3ClassiferResult = Effectiveness.classifierEffectiveness(classifier, ie, k);
                    // final double classifierF1 = classifierEffectiveness.getV1();

                    System.out.println("Best of run **********************************  classifierF1 " + t3ClassiferResult.getV1() + " " + ie.name() + '\n');

                    //  reports.reports(trainTestIndexes.v1, t6QuerySetResult.v1, t6QuerySetResult.v2, t6QuerySetResult.v3, t6QuerySetResult.v4, t6QuerySetResult.v5, t6QuerySetResult.v6, t3ClassiferResult.v1, t3ClassiferResult.v2, t3ClassiferResult.v3, ecjFitness, qType, SETK, classifyMethod, minIntersectRatio, kPenalty, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, state.generation, gaEngine, job, maxFit)
                    //reports.reports(trainTestIndexes.v1, t6QuerySetResult.v1, t6QuerySetResult.v2, t6QuerySetResult.v3, t6QuerySetResult.v4, t6QuerySetResult.v5, t6QuerySetResult.v6, t3ClassiferResult.v1, t3ClassiferResult.v2, t3ClassiferResult.v3, ecjFitness, qType, SETK, classifyMethod, minIntersectRatio, kPenalty, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, state.generation, gaEngine, job, maxFit)
                    reports.reports(ie, t6QuerySetResult.getV1(), t6QuerySetResult.getV2(), t6QuerySetResult.getV3(), t6QuerySetResult.getV4(), t6QuerySetResult.getV5(), t6QuerySetResult.getV6(), t3ClassiferResult.getV1(), t3ClassiferResult.getV2(), t3ClassiferResult.getV3(), fitness.get(), qType, SETK, classifyMethod, minIntersectRatio, kPenalty, popSize, numberOfSubPops, g.chromosome().length(), maxGene, maxGen, gaEngine, jobNumber, maxFitjob);


                    //System.out.println("statistics " + statistics);
                    //      reports.reports(ie, t6QuerySetResult, t3ClassiferResult, fitness.get(), qType, SETK, classifyMethod, minIntersectRatio, kPenalty, popSize, numberOfSubPops, g.chromosome().length(), maxGene, maxGen, gaEngine, jobNumber, 0);

                });
                reports.reportMaxFitness();
            });
        });

        final Date endRun = new Date();
        TimeDuration duration = TimeCategory.minus(endRun, startRun);
        System.out.println("Duration: " + duration);
    }

    static int getK(Genotype g, IndexEnum indexEnum, final boolean setk) {

        return (setk) ? ((IntegerChromosome) g.get(1)).gene().allele() :
                indexEnum.getNumberOfCategories();
    }
}