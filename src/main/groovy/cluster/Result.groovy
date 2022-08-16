package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum

class Result {

    final boolean setK, queryOnly, onlyDocsInOneCluster
    QType queryType
    IndexEnum indexEnum
    LuceneClassifyMethod classifyMethod
    final double v, h, c
    final double fitness
    final double kPenalty, intersectRatio
    final int clusterListSize, uniqueHits, totalHitsAllQueries
    final int popSize, job, generation

    Result(boolean setK, IndexEnum indexEnum, QType qType, double v, double h, double c, int clusterListSize, LuceneClassifyMethod classifyMethod, double fitness, boolean queryOnly, boolean onlyDocsInOneCluster, int uniqueHits, int totalHitsAllQueries, double kPenalty, double intersectRatio, int popSize, int generation, int job) {

        this.indexEnum = indexEnum
        this.setK = setK
        this.queryType = qType
        this.v = v
        this.h = h
        this.c = c
        this.classifyMethod=classifyMethod
        this.clusterListSize = clusterListSize
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

        final int numDocs = indexEnum.indexReader.numDocs()
        String setkDescription = setK ? 'k-discovered' : 'k-predefined';
        final double percentClustered = (clusterListSize / numDocs)  * 100 as Double

        if (!fcsv.exists()) {
            fcsv << 'SetK, QueryType, Index, classifyMethod, v, homogeneity, completeness, clusterListSize, fitness, queryOnly, onlyDocsInOneCluster, numDocs, uniqueHits, totalHitsAllQueries, percentClustered, kPenalty, intersectRatio, popSize, generation, job \n'  //   ,   totalUniqueHits, totalHitsAllQueries, numDocs, percentClustered, minIntersectRatio, kPenalty, useQueryOnly,onlyDocsInOneCluster, PopulationSize, Gen, Job, date \n'
        }

        fcsv <<  "$setkDescription, ${queryType.getQueryDescription()}, ${indexEnum.name()}, $classifyMethod, $v, $h, $c, $clusterListSize, $fitness, $queryOnly, $onlyDocsInOneCluster, $numDocs, $uniqueHits, $totalHitsAllQueries, $percentClustered, $kPenalty, $intersectRatio, $popSize, $generation, $job \n"  //, ${t3Uhits.v2}, ${t3Uhits.v3}, $numDocs, ${percentClustered.round(2)}, $minIntersectRatio, $kPenalty, $useQueryOnly, $onlyDocsInOneCluster, $popSize, $gen, $job, ${new Date()}  \n"
    }
}
