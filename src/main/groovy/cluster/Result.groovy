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
    final int  uniqueHits, totalHitsAllQueries, numberOfDocumentsClustered, clusterCountError
    final int popSize, job, generation
    String indexName

    Result(boolean setK, IndexEnum indexEnum, QType qType, Effectiveness effectiveness, LuceneClassifyMethod classifyMethod, double fitness, boolean queryOnly, boolean onlyDocsInOneCluster, int uniqueHits, int totalHitsAllQueries, double kPenalty, double intersectRatio, int popSize, int generation, int job) {

        indexName = indexEnum.name()
        queryTypeName = qType.getQueryDescription()
        v = effectiveness.vMeasure
        h = effectiveness.homogeniety
        c = effectiveness.completness
        clusterCountError = effectiveness.clusterCountError
        numberOfDocumentsClustered = effectiveness.numberOfDocumentsInClusters

        this.setK = setK
        this.classifyMethod=classifyMethod
        this.fitness = fitness
        this.kPenalty = kPenalty
        this.intersectRatio = intersectRatio
        this.popSize = popSize
        this.generation  = generation
        this.job = job
        this.queryOnly = queryOnly
        this.onlyDocsInOneCluster = onlyDocsInOneCluster
        this.uniqueHits = uniqueHits
        this.totalHitsAllQueries = totalHitsAllQueries
    }

    void report(File fcsv){

        final int numDocs = Indexes.indexReader.numDocs()
        String setkDescription = setK ? 'k-discovered' : 'k-predefined';
        final double percentClustered = (numberOfDocumentsClustered / numDocs)  * 100 as Double

        if (!fcsv.exists()) {
            fcsv << 'SetK, QueryType, Index, classifyMethod, v, homogeneity, completeness, numberOfDocumentsClustered, numDocs, percentClustered, clusterCountError, fitness, queryOnly, onlyDocsInOneCluster, uniqueHits, totalHitsAllQueries, kPenalty, intersectRatio, popSize, generation, job \n'  //   ,   totalUniqueHits, totalHitsAllQueries, numDocs, percentClustered, minIntersectRatio, kPenalty, useQueryOnly,onlyDocsInOneCluster, PopulationSize, Gen, Job, date \n'
        }

        fcsv <<  "$setkDescription, $queryTypeName, $indexName, $classifyMethod, $v, $h, $c, $numberOfDocumentsClustered, $numDocs, $percentClustered, $clusterCountError, $fitness, $queryOnly, $onlyDocsInOneCluster, $uniqueHits, $totalHitsAllQueries, $kPenalty, $intersectRatio, $popSize, $generation, $job \n"
    }
}
