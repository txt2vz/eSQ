package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum
import index.Indexes

class Result {

    final boolean setK, queryOnly, onlyDocsInOneCluster
    String queryTypeName
    LuceneClassifyMethod classifyMethod
    final double v, h, c
    final double fitness
    final double kPenalty, intersectRatio
    final double percentClustered
    final int k_for_knn
    final int  uniqueHits, totalHitsAllQueries, numberOfDocumentsClustered, clusterCountError
    final int popSize, job, maxFitJob, generation
    final int numDocs = Indexes.indexReader.numDocs()
    String indexName
    String setkDescription

    Result(boolean setK, IndexEnum indexEnum, QType qType, Effectiveness effectiveness, LuceneClassifyMethod classifyMethod, double fitness, boolean queryOnly, boolean onlyDocsInOneCluster, int uniqueHits, int totalHitsAllQueries, double kPenalty, double intersectRatio, int k_for_knn, int popSize, int generation, int job, int maxFitJob) {

        setkDescription = setK ? 'k-discovered' : 'k-predefined'
        indexName = indexEnum.name()
        queryTypeName = qType.getQueryDescription()
        v = effectiveness.vMeasure
        h = effectiveness.homogeneity
        c = effectiveness.completeness
        clusterCountError = effectiveness.clusterCountError
        numberOfDocumentsClustered = effectiveness.numberOfDocumentsInClusters
        percentClustered = (numberOfDocumentsClustered / numDocs)  * 100 as Double

        this.setK = setK
        this.classifyMethod = classifyMethod
        this.fitness = fitness
        this.kPenalty = kPenalty
        this.intersectRatio = intersectRatio
        this.popSize = popSize
        this.generation  = generation
        this.job = job
        this.maxFitJob = maxFitJob
        this.queryOnly = queryOnly
        this.onlyDocsInOneCluster = onlyDocsInOneCluster
        this.uniqueHits = uniqueHits
        this.totalHitsAllQueries = totalHitsAllQueries
        this.k_for_knn = k_for_knn
    }

    void report(File fcsv){

        //spreadsheet to be used with pivot table
        if (!fcsv.exists()) {
            fcsv << 'SetK,QueryType,Index,classifyMethod,v,homogeneity,completeness,numberOfDocumentsClustered,numDocs,percentClustered,clusterCountError,fitness,queryOnly,onlyDocsInOneCluster,uniqueHits,totalHitsAllQueries,kPenalty,intersectRatio,k_for_knn,popSize,generation,job,maxFitJob,Date \n'  //   ,   totalUniqueHits, totalHitsAllQueries, numDocs, percentClustered, minIntersectRatio, kPenalty, useQueryOnly,onlyDocsInOneCluster, PopulationSize, Gen, Job, date \n'
        }

        fcsv <<  "$setkDescription, $queryTypeName, $indexName, $classifyMethod, $v, $h, $c, $numberOfDocumentsClustered, $numDocs, $percentClustered, $clusterCountError, $fitness, $queryOnly, $onlyDocsInOneCluster, $uniqueHits, $totalHitsAllQueries, $kPenalty, $intersectRatio, $k_for_knn, $popSize, $generation, $job, $maxFitJob, ${new Date()} \n"
    }
}
