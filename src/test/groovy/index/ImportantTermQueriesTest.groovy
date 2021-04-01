package index

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

}
