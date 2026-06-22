package index

import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery
import spock.lang.Specification

class ImportantTermQueriesTest extends Specification {

    def "ImportantTerms NG3 tfidf"(){
        setup:
        Indexes.setIndex(IndexEnum.NG3)
        Indexes.setImportantTermQueryList(1000)

        when:
        def tfidfList = Indexes.termQueryList  
        def top10Terms = tfidfList.take(10).collect { it.getTerm().text() }

        then:
        top10Terms.contains('space')
        top10Terms.contains('god')
        top10Terms.contains('nasa')
    }
}
