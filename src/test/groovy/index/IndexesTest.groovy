package index

import spock.lang.Specification

class IndexesTest extends Specification {
    def "SetIndex"() {
    }

    def "TestSetIndex"() {
    }

    def "SetTermIntersectMap"() {

        setup:
      //  Indexes.setIndex(IndexEnum.NG3TEST, 0.2d)
        Indexes.setIndex(IndexEnum.CLASSIC4, 0.6d)

        when:
        def tfidfList = ImportantTermQueries.getTFIDFTermQueryList(Indexes.indexReader, 40)
        def tqMap = Indexes.termIntersectMap
                //Indexes.getTermIntersectMap(tfidfList, 0.3d)


        then:
       // tfidfList[0].getTerm().text() == 'nasa'
        //if (tqMap.size() >8 )
        tqMap.each {
            int sz = it.value.size()
            if (sz > 8 ){
                println " sz $sz  key: " + it.key + " value " + it.value
            }

        }
       // println "** size is " + tqMap.size() + " map is $tqMap"


    }
}
