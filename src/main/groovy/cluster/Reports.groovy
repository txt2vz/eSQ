package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.Query

class Reports {
    List<Tuple14<String, QType, String, Double, Double, Double, Double, Double, Double, Integer, Double, Integer, Double, LuceneClassifyMethod>> t14List = []

    void reports(IndexEnum ie, Tuple6<Map<Query, Integer>, Integer, Integer, Double, Double, Double> qResult, Tuple3 cResult, double fitness, QType qType, boolean setk, LuceneClassifyMethod lcm, double minIntersectRatio, int popSize, int numberOfSubpops, int genomeSize, int maxGene, int gen, String gaEngine, int job, int maxFitJob) {

        Map<Query, Integer> queryMap = qResult.v1
        final int uniqueHits = qResult.v2
        final int totalHits = qResult.v3
        final double qF1 = qResult.v4
        final double qP = qResult.v5
        final double qR = qResult.v6

        final double cF1 = cResult.v1
        final double cP = cResult.v2
        final double cR = cResult.v3

        final int numberOfClusters = queryMap.size();
        final int categoryCountError = ie.numberOfCategories - numberOfClusters
        final int categoryCountErrorAbs = Math.abs(categoryCountError)

        String setkDescription = setk ? 'k-discovered' : 'k-predefined';

        File fcsv = new File("results/results.csv")
        if (!fcsv.exists()) {
            fcsv << 'SetK, QueryType, Index, QueryF1, QueryPrecision, QueryRecall, ClassifierF1,ClassifierPrecision,ClassifierRecall, UniqueHits, Fitness, NumberofCategories, NumberOfClusters, ClusterCountError, ClassifyMethod, MinIntersectRatio, PopulationSize, NumberOfSubPops, GenomeSize, MaxGene, Gen, GA_Engine, Job, maxFitJob, date \n'
        }
//        fcsv << " $setk, $qType, ${ie.name()}, $qF1, $qP, $qR, $cF1, $cR, $cP, $uniqueHits, $fitness, $ie.numberOfCategories, $numberOfClusters, $categoryCountErrorAbs, $lcm, $onlyDocsInOnecluster, $popSize, $numberOfSubpops, $genomeSize, $maxGene, $gen, $gaEngine, $job, $maxFitJob, ${new Date()} \n"
        fcsv << " $setkDescription, ${qType.getQueryDescription()}, ${ie.name()}, $qF1, $qP, $qR, $cF1, $cR, $cP, $uniqueHits, $fitness, $ie.numberOfCategories, $numberOfClusters, $categoryCountErrorAbs, $lcm, $minIntersectRatio, $popSize, $numberOfSubpops, $genomeSize, $maxGene, $gen, $gaEngine, $job, $maxFitJob, ${new Date()} \n"


        File queryFileOut = new File('results/Queries.txt')
        queryFileOut << "Total Docs: ${Indexes.indexReader.numDocs()} Index: ${Indexes.index} ${new Date()} \n"
        queryFileOut << "UniqueHits: ${uniqueHits}  TotalHitsAllQueries: $totalHits  QuerySetf1: $qF1 ClassifierF1: $cF1 setk: $setk CategoryCountError: $categoryCountErrorAbs  minIntersectRation: $minIntersectRatio \n"
        queryFileOut << QuerySet.printQuerySet(queryMap)
        queryFileOut << "************************************************ \n \n"

        t14List << new Tuple14(setkDescription, qType, ie.name(), qF1, qP, qR, cF1, cP, cR, categoryCountErrorAbs, minIntersectRatio, uniqueHits, fitness, lcm)
    }

    void reportMaxFitness() {

        File fcsvMax = new File("results/maxFitnessReport.csv")
        if (!fcsvMax.exists()) {
            fcsvMax << 'Setk, QueryType, Index, queryF1, queryPrecision, queryRecall, ClassifierF1, ClassifierPrecision, ClasssifierRecall, CategoryCountError, MinIntersectRatio, UniqueHits, Fitness, ClassifyMethod, Date \n'
        }

        t14List.toUnique { it.v1 }.each { t ->
            def t13Max = t14List.findAll { t.v3 == it.v3 }.max { q -> q.v13 }
            fcsvMax << "${t13Max.v1}, ${t13Max.v2.getQueryDescription()}, ${t13Max.v3}, ${t13Max.v4},${t13Max.v5},${t13Max.v6},${t13Max.v7},${t13Max.v8}, ${t13Max.v9},${t13Max.v10},${t13Max.v11}, ${t13Max.v12}, ${t13Max.v13},  ${new Date()} \n"
        }

        println "Average query f1 " + t14List.average { it.v4 } + " Classifier f1: " + t14List.average { it.v7 }
        t14List.clear();
    }
}
