package index

import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery
import spock.lang.Specification

class ImportantTermQueriesTest extends Specification {

    def "ImportantTerms NG3 tfidf"(){
        setup:
        Indexes.setIndex(IndexEnum.NG3)

        when:
        def tfidfList = ImportantTermQueries.getTFIDFTermQueryList(Indexes.indexReader)

        then:
        tfidfList[0].getTerm().text() == 'god'
        tfidfList[1].getTerm().text() == 'space'
        tfidfList[2].getTerm().text() == 'jesus'
    }

//    def "Term Query Intersect Map Sorted "(){
//
//        setup:
//        Indexes.setIndex(IndexEnum.NG3)
//
//        when:
//        def tqList = ImportantTermQueries.getTFIDFTermQueryList(Indexes.indexReader)
//        def tqMapSorted = ImportantTermQueries.getTermIntersectMapSorted(tqList)
//        def tqOrbit = new TermQuery(new Term(Indexes.FIELD_CONTENTS,'orbit'))
//
//        then:
//        println "tqMapSorted (20): ${tqMapSorted.take(20)}"
//        tqMapSorted[tqOrbit].first().v1.getTerm().text() == 'lunar'
//        tqMapSorted[tqOrbit].first().v2 == 0.75
//    }
}
