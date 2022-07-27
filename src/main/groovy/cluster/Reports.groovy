package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.Query

class Reports {
    List<Tuple15<String, QType, String, Double, Double, Double, Double, Double, Double, Integer, Double, Integer, Double, LuceneClassifyMethod, Double>> t15List = []

   void reports(IndexEnum ie,Map<Query, Integer> queryMap,int uniqueHits, int totalHits, double qF1, double qP, double qR, double cF1, double cP, double cR,   double fitness, QType qType, boolean setk, LuceneClassifyMethod lcm, double minIntersectRatio, double kPenalty, int popSize, int numberOfSubpops, int genomeSize, int maxGene, int gen, String gaEngine, int job, int maxFitJob) {

        final int numberOfClusters = queryMap.size();
        final int categoryCountError = ie.numberOfCategories - numberOfClusters
        final int categoryCountErrorAbs = Math.abs(categoryCountError)

        String setkDescription = setk ? 'k-discovered' : 'k-predefined';

        File fcsv = new File("results/results.csv")
        if (!fcsv.exists()) {
            fcsv << 'SetK, QueryType, Index, QueryF1, QueryPrecision, QueryRecall, ClassifierF1,ClassifierPrecision,ClassifierRecall, UniqueHits, Fitness, NumberofCategories, NumberOfClusters, ClusterCountError,  ClassifyMethod, MinIntersect, kPenalty, PopulationSize, NumberOfSubPops, GenomeSize, MaxGene, MinIntersectRatio, Gen, GA_Engine, Job, maxFitJob, date \n'
        }
        fcsv << " $setkDescription, ${qType.getQueryDescription()}, ${ie.name()}, $qF1, $qP, $qR, $cF1, $cR, $cP, $uniqueHits, $fitness, $ie.numberOfCategories, $numberOfClusters, $categoryCountErrorAbs, $lcm, $minIntersectRatio, $kPenalty, $popSize, $numberOfSubpops, $genomeSize, $maxGene, $minIntersectRatio, $gen, $gaEngine, $job, $maxFitJob, ${new Date()} \n"

        File queryFileOut = new File('results/Queries.txt')
        queryFileOut << "Total Docs: ${Indexes.indexReader.numDocs()} Index: ${Indexes.index} ${new Date()} \n"
        queryFileOut << "UniqueHits: ${uniqueHits}  TotalHitsAllQueries: $totalHits  QuerySetf1: $qF1  qP: $qP qR: $qR ClassifierF1: $cF1  cP: $cP cR: $cR  setk: $setk CategoryCountError: $categoryCountErrorAbs  minIntersectRation: $minIntersectRatio \n"
        queryFileOut << QuerySet.printQuerySet(queryMap)
        queryFileOut << "************************************************ \n \n"

        t15List << new Tuple15(setkDescription, qType, ie.name(), qF1, qP, qR, cF1, cP, cR, categoryCountErrorAbs, minIntersectRatio, uniqueHits, fitness, lcm, kPenalty)
    }

    void reportMaxFitness(int job) {

        File fcsvMax = new File("results/maxFitnessReport.csv")
        if (!fcsvMax.exists()) {
            fcsvMax << 'Setk, QueryType, Index, queryF1, queryPrecision, queryRecall, ClassifierF1, ClassifierPrecision, ClasssifierRecall, CategoryCountError, MinIntersect, UniqueHits, Fitness, ClassifyMethod, kPenalty, Job, Date \n'
        }

        t15List.toUnique { it.v1 }.each { t ->
            def t15Max = t15List.findAll { t.v3 == it.v3 }.max { q -> q.v13 }
            fcsvMax << "${t15Max.v1}, ${t15Max.v2.getQueryDescription()}, ${t15Max.v3}, ${t15Max.v4},${t15Max.v5},${t15Max.v6},${t15Max.v7},${t15Max.v8}, ${t15Max.v9},${t15Max.v10},${t15Max.v11}, ${t15Max.v12}, ${t15Max.v13}, ${t15Max.v14}, ${t15Max.v15}, ${job},  ${new Date()} \n"
        }

        //println "Job Average query f1  for category" + t15List.average { it.v4 } + " Classifier f1: " + t15List.average { it.v7 }
       // println "T15 $t15List"

        t15List.clear();
    }

    void reportV(IndexEnum ie, QType qType, boolean setK, LuceneClassifyMethod lcm, int popSize, int job, int gen, t3){

        String setkDescription = setK ? 'k-discovered' : 'k-predefined';
        File fcsv = new File("results/resultsV.csv")
        if (!fcsv.exists()) {
            fcsv << 'SetK, QueryType, Index, classifyMethod, v, homogeneity, completeness, PopulationSize, Gen, Job, date \n'
        }
        fcsv <<  "$setkDescription, ${qType.getQueryDescription()}, ${ie.name()}, $lcm, ${t3.v1}, ${t3.v2}, ${t3.v3}, $popSize, $gen, $job, ${new Date()}  \n"
                //" $setkDescription, ${qType.getQueryDescription()}, ${ie.name()}, $qF1, $qP, $qR, $cF1, $cR, $cP, $uniqueHits, $fitness, $ie.numberOfCategories, $numberOfClusters, $categoryCountErrorAbs, $lcm, $minIntersectRatio, $kPenalty, $popSize, $numberOfSubpops, $genomeSize, $maxGene, $minIntersectRatio, $gen, $gaEngine, $job, $maxFitJob, ${new Date()} \n"
    }
}
