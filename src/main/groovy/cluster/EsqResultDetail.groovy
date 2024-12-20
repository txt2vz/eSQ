package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.Query

class EsqResultDetail {

    final boolean onlyDocsInOneCluster
   // String queryTypeName, queryOnlyString
    LuceneClassifyMethod classifyMethod
    final double v, h, c, adjusted_rand
    final double fitness
    final double kPenalty, intersectRatio
    final double percentClusteredByQuery
    final int k_for_knn
    final int uniqueHits, totalHitsAllQueries, numberOfDocumentsClusteredByQuery, clusterCountError, numberOfClusters, numberOfClasses
    final int popSize, job, maxFitJob, generation
    final int numDocs = Indexes.indexReader.numDocs()
    final int absClusterCountError
    String indexName
    String gaEngine
    Map<Query, Integer> queryMap

    EsqResultDetail(IndexEnum indexEnum, Effectiveness effectiveness, double fitness, QuerySet querySet, LuceneClassifyMethod classifyMethod, boolean queryOnly, boolean onlyDocsInOneCluster, double kPenalty, double intersectRatio, int k_for_knn, int popSize, int generation, int job, int maxFitJob, String gaEngine) {

        indexName = indexEnum.name()
        v = effectiveness.vMeasure
        h = effectiveness.homogeneity
        c = effectiveness.completeness
        adjusted_rand = effectiveness.adjusted_rand
        clusterCountError = effectiveness.clusterCountError
        numberOfClusters = effectiveness.numberOfClusters
        numberOfClasses = effectiveness.numberOfClasses
        numberOfDocumentsClusteredByQuery = effectiveness.numberOfDocumentsInQueryBuiltClusters
        percentClusteredByQuery = (numberOfDocumentsClusteredByQuery / numDocs)  * 100 as Double
//        queryOnlyString = queryOnly ? 'docsMatchingQueryOnly' : 'allDocuments'
        absClusterCountError = Math.abs(clusterCountError)

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
    }

    void queryReport(File f){
        StringBuilder sb = new StringBuilder()
        queryMap.keySet().eachWithIndex { Query q, int index ->
            sb << "Query: $index :  ${queryMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)} \n"
        }

        f << "$indexName Fitness: $fitness $intersectRatio v: $v  h: $h  c: $c  kPenalty $kPenalty: intersectRation: $intersectRatio  clusterCountError $clusterCountError\n"
        f<< sb.toString()
        f <<'\n'
    }

    void report(File fcsv){

        //spreadsheet to be used with pivot table
        if (!fcsv.exists()) {
            fcsv << 'Index,v,homogeneity,completeness,adjusted_rand,fitness,numberOfDocumentsClusteredByQuery,numDocs,percentClusteredByQuery,numberOfClasses,numberOfClusters,clusterCountError,absClusterCountError,useNonIntersectingClustersForTrainingKNN,uniqueHits,totalHitsAllQueries,kPenalty,intersectRatio,k_for_knn,popSize,generation,job,maxFitJob,GA_Engine,Date \n'

        }

        fcsv << "$indexName, $v, $h, $c, $adjusted_rand, $fitness, $numberOfDocumentsClusteredByQuery, $numDocs, $percentClusteredByQuery, $numberOfClasses, $numberOfClusters, $clusterCountError, $absClusterCountError, $onlyDocsInOneCluster, $uniqueHits, $totalHitsAllQueries, $kPenalty, $intersectRatio, $k_for_knn, $popSize, $generation, $job, $maxFitJob,$gaEngine, ${new Date()} \n"
    }
}
