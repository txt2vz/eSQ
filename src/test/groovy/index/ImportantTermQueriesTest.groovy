package index

import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery
import spock.lang.Specification

class ImportantTermQueriesTest extends Specification {

    def "ImportantTerms NG3TEST tfidf"(){
        setup:
        Indexes.setIndex(IndexEnum.NG3TEST)

        when:
        def tfidfList = ImportantTermQueries.getTFIDFTermQueryList(Indexes.indexReader)

        then:
        tfidfList[0].getTerm().text() == 'nasa'
        tfidfList[1].getTerm().text() == 'space'
        tfidfList[2].getTerm().text() == 'god'
    }

    def "Term Query Intersect Map Sorted "(){

        setup:
        Indexes.setIndex(IndexEnum.NG3TEST, 0.2d)

        when:
        def tqList = ImportantTermQueries.getTFIDFTermQueryList(Indexes.indexReader)
        def tqMapSorted = ImportantTermQueries.getTermIntersectMapSorted(tqList, 0.7d)
        def tqOrbit = new TermQuery(new Term(Indexes.FIELD_CONTENTS,'orbit'))

        then:
        println "tqMapSorted: $tqMapSorted"
        tqMapSorted[tqOrbit].first().v1.getTerm().text() == 'lunar'
        tqMapSorted[tqOrbit].first().v2 == 0.75
    }
}
