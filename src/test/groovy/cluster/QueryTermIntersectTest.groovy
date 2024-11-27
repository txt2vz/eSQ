package cluster

import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import spock.lang.Specification

class QueryTermIntersectTest extends Specification {

    def "QueryListFromChromosome OR NG3 tfidf"() {
        setup:
        Indexes.setIndex(IndexEnum.NG3, 0.0d)

        when:
        final int k = 3
        int[] genome3 =  new int[] {0, 1, 2}
        List<BooleanQuery.Builder> bqbL =  QueryBuilders.getQueryBuilderArray(genome3, k, QType.OR1)
        Query q0 = bqbL[0].build()
        Query q1 = bqbL[1].build()
        Query q2 = bqbL[2].build()

        then:
        Indexes.termQueryList[0].getTerm().text() == 'god'
        Indexes.termQueryList[1].getTerm().text() == 'space'
        Indexes.termQueryList[2].getTerm().text() == 'jesus'
        Indexes.termQueryList[3].getTerm().text() == 'hockey'

        bqbL.size() ==  Indexes.index.numberOfCategories
        q0.toString(Indexes.FIELD_CONTENTS) == 'god'
        q1.toString(Indexes.FIELD_CONTENTS) == 'space'
        q2.toString(Indexes.FIELD_CONTENTS) == 'jesus'

        when:
        int[] genome7 = [3, 0, 1, 4, 7, 3, 2] as int[]
      // bqbL = QueryBuilders.getQueryBuilderArray(genome6, k, QType.OR_INTERSECT)
         bqbL = QueryBuilders.getMultiWordQuery(genome7, k, QType.OR_INTERSECT)

        Query q3 = bqbL[0].build()
        print("ffff " + q3.toString(Indexes.FIELD_CONTENTS))
        Query q4 = bqbL[1].build()
        Query q5 = bqbL[2].build()

        then:
        q3.toString(Indexes.FIELD_CONTENTS) == 'space god'
//        q4.toString(Indexes.FIELD_CONTENTS) == 'god hockey'
//        q5.toString(Indexes.FIELD_CONTENTS) == 'orbit game'

        when:
        Indexes.MIN_INTERSECT_RATIO = 0.2d
        genome3 = [0, 2, 4, 1, 3, 7] as int[]
        bqbL = QueryBuilders.getQueryBuilderArray(genome3, k, QType.OR_INTERSECT)

        Query q6 = bqbL[0].build()
        Query q7 = bqbL[1].build()
        Query q8 = bqbL[2].build()

        then:
        q6.toString(Indexes.FIELD_CONTENTS) == 'space'
//        q7.toString(Indexes.FIELD_CONTENTS) == 'god'
//        q8.toString(Indexes.FIELD_CONTENTS) == 'orbit'
    }
}


