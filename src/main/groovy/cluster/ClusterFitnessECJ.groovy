package cluster

import ec.simple.SimpleFitness
import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query

@CompileStatic
public class ClusterFitnessECJ extends SimpleFitness {

    Map<Query, Integer> queryMap//= [:]
    BooleanQuery.Builder[] arrayOfQueryBuilders
    double baseFitness = 0.0  //for ECJ
    int uniqueHits = 0
    int totalHits = 0
    int k

    double getFitness() {
        return baseFitness;
    }

    void setClusterFitness(QuerySet querySetFeatures, BooleanQuery.Builder[] arrayOfQueryBuilders, double f) {

        this.arrayOfQueryBuilders = arrayOfQueryBuilders
        baseFitness = f
        queryMap= querySetFeatures.queryMap
        uniqueHits = querySetFeatures.totalHitsReturnedByOnlyOneQuery
        totalHits = querySetFeatures.totalHitsAllQueries
        k = this.arrayOfQueryBuilders.size()
    }

    void generationStats(long generation) {
        println "${queryShort()}"
        println "baseFitness: ${baseFitness.round(3)} uniqueHits: $uniqueHits  totalHits: $totalHits totalDocs: ${Indexes.indexReader.numDocs()} "
        println ""
    }

    String queryShort() {
        StringBuilder sb = new StringBuilder()
        queryMap.keySet().eachWithIndex { Query q, int index ->
            sb << "ClusterQuery: $index :  ${queryMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)} \n"
        }
        return sb.toString()
    }

    //sent to stat file in statDump
    public String fitnessToStringForHumans() {
        return "ClusterQuery Fitness: ${this.fitness()}"
    }

    public String toString(int gen) {
        return "Gen: $gen ClusterQuery Fitness: ${this.fitness()} queryMap: $queryMap}"
    }
}