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
}
