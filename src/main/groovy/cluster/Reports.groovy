package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.Query

class Reports {

    List <Tuple6 <Double, IndexEnum, QType, String, LuceneClassifyMethod, Double>> tList = []

    void reportMaxFitness(int job) {

        File fcsvMax = new File("results/maxFitnessReportN.csv")
        if (!fcsvMax.exists()) {
        //    fcsvMax << 'Setk, QueryType, Index, queryF1, queryPrecision, queryRecall, ClassifierF1, ClassifierPrecision, ClasssifierRecall, CategoryCountError, MinIntersect, UniqueHits, Fitness, ClassifyMethod, kPenalty, Job, Date \n'
               fcsvMax << 'Setk, QueryType, Index,  ClassifyMethod, v, percent, Date \n'

        }

        tList.toUnique { it.v1 }.each { t ->
            def tMax = tList.findAll { t.v2 == it.v2 }.max { q -> q.v1 }
            fcsvMax <<  "${tMax.v3}, ${tMax.v2}, ${tMax.v4}, ${tMax.v5}, ${tMax.v6}, ${new Date()} \n"
                    //"${t15Max.v1}, ${t15Max.v2.getQueryDescription()}, ${t15Max.v3}, ${t15Max.v4},${t15Max.v5},${t15Max.v6},${t15Max.v7},${t15Max.v8}, ${t15Max.v9},${t15Max.v10},${t15Max.v11}, ${t15Max.v12}, ${t15Max.v13}, ${t15Max.v14}, ${t15Max.v15}, ${job},  ${new Date()} \n"
        }

        //println "Job Average query f1  for category" + t15List.average { it.v4 } + " Classifier f1: " + t15List.average { it.v7 }
       // println "T15 $t15List"

        tList.clear();
    }

    void reportV(double fitness, IndexEnum ie, QType qType, boolean setK, LuceneClassifyMethod lcm, double minIntersectRatio, double kPenalty, int popSize, int job, int gen, Tuple4 t4vhc, Tuple3 t3Uhits, boolean  useQueryOnly, boolean  onlyDocsInOneCluster ){

        String setkDescription = setK ? 'k-discovered' : 'k-predefined';
        File fcsv = new File("results/resultsV.csv")
        final int numDocs = ie.indexReader.numDocs()
        int clusterLabelCount = t4vhc.v4

        double percentClustered = (clusterLabelCount / numDocs)  * 100 as Double

        if (!fcsv.exists()) {
            fcsv << 'SetK, QueryType, Index, classifyMethod, v, homogeneity, completeness, clusterLabelsCount, totalUniqueHits, totalHitsAllQueries, numDocs, percentClustered, minIntersectRatio, kPenalty, useQueryOnly,onlyDocsInOneCluster, PopulationSize, Gen, Job, date \n'
        }

        fcsv <<  "$setkDescription, ${qType.getQueryDescription()}, ${ie.name()}, $lcm, ${t4vhc.v1}, ${t4vhc.v2}, ${t4vhc.v3}, ${t4vhc.v4}, ${t3Uhits.v2}, ${t3Uhits.v3}, $numDocs, ${percentClustered.round(2)}, $minIntersectRatio, $kPenalty, $useQueryOnly, $onlyDocsInOneCluster, $popSize, $gen, $job, ${new Date()}  \n"
        tList << new Tuple6 (fitness, qType, setkDescription, lcm, t4vhc.v1, percentClustered)
    }
}
