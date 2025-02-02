package cluster

import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import spock.lang.Specification

class QueryTermIntersectTest extends Specification {

    def "QueryListFromChromosome OR NG3 tfidf"() {
        setup:
        Indexes.setIndex(IndexEnum.NG3)

        when:
        int k = 3
        int[] genome3 =  new int[] {0, 1, 2}

        List<BooleanQuery.Builder> bqbL =  EsqQueryBuilder.getMultiWordQueryModulusDuplicateCheck(genome3, Indexes.termQueryList, k)
        Query q0 = bqbL[0].build()
        Query q1 = bqbL[1].build()
        Query q2 = bqbL[2].build()

        then:
        Indexes.termQueryList[0].getTerm().text() == 'god'
        Indexes.termQueryList[1].getTerm().text() == 'space'
        Indexes.termQueryList[2].getTerm().text() == 'jesus'
        Indexes.termQueryList[3].getTerm().text() == 'hockey'
        Indexes.termQueryList[4].getTerm().text() == 'nasa'
        Indexes.termQueryList[5].getTerm().text() == 'game'
        Indexes.termQueryList[6].getTerm().text() == 'team'

        bqbL.size() ==  Indexes.index.numberOfCategories
        q0.toString(Indexes.FIELD_CONTENTS) == 'god'
        q1.toString(Indexes.FIELD_CONTENTS) == 'space'
        q2.toString(Indexes.FIELD_CONTENTS) == 'jesus'

//repeated words should not be added
        when:
        k = 3
        int[] genome6 =  new int[] {0, 1, 2, 0, 1, 2}

        bqbL =  EsqQueryBuilder.getMultiWordQueryModulusDuplicateCheck(genome3, Indexes.termQueryList, k)
         q0 = bqbL[0].build()
         q1 = bqbL[1].build()
         q2 = bqbL[2].build()

        then:
        q0.toString(Indexes.FIELD_CONTENTS) == 'god'
        q1.toString(Indexes.FIELD_CONTENTS) == 'space'
        q2.toString(Indexes.FIELD_CONTENTS) == 'jesus'

        when:
        k = 3
        genome6 =  new int[] {0, 1, 2, 3, 4, 5}
        List<BooleanQuery.Builder> bqbL6 =  EsqQueryBuilder.getMultiWordQueryModulusDuplicateCheck(genome6, Indexes.termQueryList, k)

        Query q3 = bqbL6[0].build()

        then:
        q3.toString(Indexes.FIELD_CONTENTS) == 'god'

        when:
        k = 3
        genome6 =  new int[] {0, 1, 2, 7, 4, 2}

        bqbL =  EsqQueryBuilder.getMultiWordQueryModulusDuplicateCheck(genome6, Indexes.termQueryList, k)
        q0 = bqbL[0].build()
        q1 = bqbL[1].build()
        q2 = bqbL[2].build()

        then:
        q0.toString(Indexes.FIELD_CONTENTS) == 'god christians'
        q1.toString(Indexes.FIELD_CONTENTS) == 'space nasa'
        q2.toString(Indexes.FIELD_CONTENTS) == 'jesus'


        when:
        k = 4
        genome6 =  new int[] {0, 1, 2, 14, 17, 4, 4, 4}

        bqbL =  EsqQueryBuilder.getMultiWordQueryModulusDuplicateCheck(genome6, Indexes.termQueryList, k)
        q0 = bqbL[0].build()
        q1 = bqbL[1].build()
        q2 = bqbL[2].build()

        then:
        q1.toString(Indexes.FIELD_CONTENTS) == 'space nasa'
    }
}


