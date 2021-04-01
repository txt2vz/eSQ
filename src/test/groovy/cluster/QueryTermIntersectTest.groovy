package cluster

import index.ImportantTermQueries
import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import spock.lang.Specification

class QueryTermIntersectTest extends Specification {

    def "QueryListFromChromosome OR 20News3 tfidf"() {
        setup:
        Indexes.setIndex(IndexEnum.NG3TEST)
        List <TermQuery> tfidfList =
                ImportantTermQueries.getTFIDFTermQueryList(Indexes.indexReader) asImmutable()
        int[] genome =  new int[] {0, 1, 2}

        when:
        final int k = 3
        List<BooleanQuery.Builder> bqbL =    QuerySet.getQueryBuilderList(genome, tfidfList, k, QType.OR1)

        Query q = bqbL[0].build()
        String s = q.toString(Indexes.FIELD_CONTENTS)
        println "s $s"
        TermQuery tq = new TermQuery (new Term(Indexes.FIELD_CONTENTS,s))
        println "tq $tq"

        then:
        Indexes.index.numberOfCategories == 3

        tfidfList[0].getTerm().text() == 'nasa'
        tfidfList[1].getTerm().text() == 'space'
        tfidfList[2].getTerm().text() == 'god'

        bqbL.size() ==  Indexes.index.numberOfCategories
        q.toString(Indexes.FIELD_CONTENTS) == 'nasa'

        when:
        genome = [0, 2, 4, 1, 3, 7] as int[]
        QueryTermIntersect.minIntersect = MinIntersectValue.NONE
        bqbL =    QuerySet.getQueryBuilderList(genome, tfidfList, k, QType.OR_INTERSECT)

        Query q0 = bqbL[0].build()
        Query q1 = bqbL[1].build()
        Query q2 = bqbL[2].build()


//        println "qo $q0"
//        println "q1 $q1"
//        println "q2 $q2"

        then:
        q0.toString(Indexes.FIELD_CONTENTS) == 'nasa space'
        q1.toString(Indexes.FIELD_CONTENTS) == 'god hockey'
        q2.toString(Indexes.FIELD_CONTENTS) == 'orbit game'
    }
}


