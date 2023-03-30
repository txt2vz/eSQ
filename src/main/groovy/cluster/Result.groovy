package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.Query

class Result {

    final boolean setK, onlyDocsInOneCluster
    String queryTypeName, queryOnlyString
    LuceneClassifyMethod classifyMethod
    final double v, h, c, adjusted_rand
    final double fitness
    final double kPenalty, intersectRatio
    final double percentClustered
    final int k_for_knn
    final int  uniqueHits, totalHitsAllQueries, numberOfDocumentsClustered, clusterCountError, numberOfClusters, numberOfClasses
    final int popSize, job, maxFitJob, generation
    final int numDocs = Indexes.indexReader.numDocs()
    final int absClusterCountError
    String indexName
    String setkDescription
    String gaEngine
    Map<Query, Integer> queryMap


    Result(boolean setK, IndexEnum indexEnum, QType qType, Effectiveness effectiveness, double fitness,  QuerySet querySet, LuceneClassifyMethod classifyMethod, boolean queryOnly, boolean onlyDocsInOneCluster, double kPenalty, double intersectRatio, int k_for_knn, int popSize, int generation, int job, int maxFitJob, String gaEngine) {

        setkDescription = setK ? 'k-discovered' : 'k-predefined'
        indexName = indexEnum.name()
        queryTypeName = qType.getQueryDescription()
        v = effectiveness.vMeasure
        h = effectiveness.homogeneity
        c = effectiveness.completeness
        adjusted_rand = effectiveness.adjusted_rand
        clusterCountError = effectiveness.clusterCountError

        numberOfDocumentsClustered = effectiveness.numberOfDocumentsInClusters
        numberOfClusters = effectiveness.numberOfClusters
        numberOfClasses = effectiveness.numberOfClasses
        percentClustered = (numberOfDocumentsClustered / numDocs)  * 100 as Double
        queryOnlyString = queryOnly ? 'docsMatchingQueryOnly' : 'allDocuments'
        absClusterCountError = Math.abs(clusterCountError)


        this.setK = setK
        this.classifyMethod = classifyMethod
        this.fitness = fitness
        this.kPenalty = kPenalty
        this.intersectRatio = intersectRatio
        this.popSize = popSize
        this.generation  = generation
        this.job = job
        this.maxFitJob = maxFitJob
        this.onlyDocsInOneCluster = onlyDocsInOneCluster
        this.uniqueHits =  querySet.totalHitsReturnedByOnlyOneQuery//uniqueHits
        this.totalHitsAllQueries =  querySet.totalHitsAllQueries   //totalHitsAllQueries
        this.k_for_knn = k_for_knn
        this.queryMap = querySet.getQueryMap()
        this.gaEngine = gaEngine


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
            fcsv << 'SetK,QueryType,Index,classifyMethod,v,homogeneity,completeness,adjusted_rand,fitness,numberOfDocumentsClustered,numDocs,percentClustered,numberOfClasses,numberOfClusters,clusterCountError,absClusterCountError,queryOnly,useNonIntersectingClustersForTrainingKNN,uniqueHits,totalHitsAllQueries,kPenalty,intersectRatio,k_for_knn,popSize,generation,job,maxFitJob,GA_Engine,Date \n'

        }

        fcsv << "$setkDescription, $queryTypeName, $indexName, $classifyMethod, $v, $h, $c, $adjusted_rand, $fitness, $numberOfDocumentsClustered, $numDocs, $percentClustered, $numberOfClasses, $numberOfClusters, $clusterCountError, $absClusterCountError, $queryOnlyString, $onlyDocsInOneCluster, $uniqueHits, $totalHitsAllQueries, $kPenalty, $intersectRatio, $k_for_knn, $popSize, $generation, $job, $maxFitJob,$gaEngine, ${new Date()} \n"
    }
}
