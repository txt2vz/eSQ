package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.Query

class Result {

    final boolean setK, onlyDocsInOneCluster
    String queryTypeName, queryOnlyString
    LuceneClassifyMethod classifyMethod
    final double v, h, c
    final double fitness
    final double kPenalty, intersectRatio
    final double percentClustered
    final int k_for_knn
    final int  uniqueHits, totalHitsAllQueries, numberOfDocumentsClustered, clusterCountError, numberOfClusters, numberOfClasses
    final int popSize, job, maxFitJob, generation
    final int numDocs = Indexes.indexReader.numDocs()
    String indexName
    String setkDescription
    Map<Query, Integer> queryMap

    Result(boolean setK, IndexEnum indexEnum, QType qType, Effectiveness effectiveness, LuceneClassifyMethod classifyMethod, double fitness, boolean queryOnly, boolean onlyDocsInOneCluster, int uniqueHits, int totalHitsAllQueries, double kPenalty, double intersectRatio, int k_for_knn,  Map<Query, Integer> queryMap, int popSize, int generation, int job, int maxFitJob) {

        setkDescription = setK ? 'k-discovered' : 'k-predefined'
        indexName = indexEnum.name()
        queryTypeName = qType.getQueryDescription()
        v = effectiveness.vMeasure
        h = effectiveness.homogeneity
        c = effectiveness.completeness
        clusterCountError = effectiveness.clusterCountError

        numberOfDocumentsClustered = effectiveness.numberOfDocumentsInClusters
        numberOfClusters = effectiveness.numberOfClusters
        numberOfClasses = effectiveness.numberOfClasses
        percentClustered = (numberOfDocumentsClustered / numDocs)  * 100 as Double

        queryOnlyString = queryOnly ? 'docsMatchingQueryOnly' : 'allDocuments'

        this.setK = setK
        this.classifyMethod = classifyMethod
        this.fitness = fitness
        this.kPenalty = kPenalty
        this.intersectRatio = intersectRatio
        this.popSize = popSize
        this.generation  = generation
        this.job = job
        this.maxFitJob = maxFitJob
       // this.queryOnly = queryOnly
        this.onlyDocsInOneCluster = onlyDocsInOneCluster
        this.uniqueHits = uniqueHits
        this.totalHitsAllQueries = totalHitsAllQueries
        this.k_for_knn = k_for_knn
        this.queryMap = queryMap

        if (!setK && clusterCountError != 0){
            println "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX "
            report(new File("error.csv"))
            queryReport(new File("error.txt"))
        }
        if (!setK) assert clusterCountError == 0
    }

    void queryReport(File f){
        StringBuilder sb = new StringBuilder()
        queryMap.keySet().eachWithIndex { Query q, int index ->
            sb << "Query: $index :  ${queryMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)} \n"
        }

        f << "$setkDescription $queryTypeName $indexName intersectRatio: $intersectRatio $queryOnlyString v: $v  h: $h  c: $c  kPenalty $kPenalty: intersectRation: $intersectRatio  clusterCountError $clusterCountError\n"
        f<< sb.toString()
        f <<'\n'
    }

    void report(File fcsv){

        //spreadsheet to be used with pivot table
        if (!fcsv.exists()) {
            fcsv << 'SetK,QueryType,Index,classifyMethod,v,homogeneity,completeness,numberOfDocumentsClustered,numDocs,percentClustered,numberOfClasses,numberOfClusters,clusterCountError,fitness,queryOnly,onlyDocsInOneCluster,uniqueHits,totalHitsAllQueries,kPenalty,intersectRatio,k_for_knn,popSize,generation,job,maxFitJob,Date \n'  //   ,   totalUniqueHits, totalHitsAllQueries, numDocs, percentClustered, minIntersectRatio, kPenalty, useQueryOnly,onlyDocsInOneCluster, PopulationSize, Gen, Job, date \n'
        }

        fcsv << "$setkDescription, $queryTypeName, $indexName, $classifyMethod, $v, $h, $c, $numberOfDocumentsClustered, $numDocs, $percentClustered, $numberOfClasses, $numberOfClusters, $clusterCountError, $fitness, $queryOnlyString, $onlyDocsInOneCluster, $uniqueHits, $totalHitsAllQueries, $kPenalty, $intersectRatio, $k_for_knn, $popSize, $generation, $job, $maxFitJob, ${new Date()} \n"
    }
}
