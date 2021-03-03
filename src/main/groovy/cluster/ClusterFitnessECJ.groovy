package cluster

import ec.simple.SimpleFitness
import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TotalHitCountCollector

@CompileStatic
public class ECJclusterFitness extends SimpleFitness {

    public static double K_PENALTY = 0.04d

    Map<Query, Integer> queryMap = [:]
    List<BooleanQuery.Builder> bqbList =[]
    double baseFitness = 0.0  //for ECJ
    int uniqueHits = 0
//    int hitsMatchingTwoOrMoreQueries = 0
    int totalHits = 0
  //  int missedDocs = 0
    int k

    double getFitness() {
        return baseFitness;
    }

    void setClusterFitness( Tuple3 <Map<Query, Integer>, Integer, Integer> t3UniqueHits , List<BooleanQuery.Builder> bqbListIn, double f) {

        bqbList = bqbListIn
        baseFitness = f
        queryMap= t3UniqueHits.v1
        uniqueHits = t3UniqueHits.v2
        totalHits = t3UniqueHits.v3
        k = bqbList.size()
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
        return "Gen: $gen ClusterQuery Fitness: ${this.fitness()} qMap: $queryMap}"
    }
}