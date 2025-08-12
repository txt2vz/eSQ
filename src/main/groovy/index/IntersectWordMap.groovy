package index

import cluster.QueryTermIntersect
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.CompileStatic
import org.apache.lucene.index.*
import org.apache.lucene.search.DocIdSetIterator
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.util.BytesRef

@CompileStatic
class IntersectWordMap {

    final static int MAX_TERMQUERYLIST_SIZE = 120

    Map<String, List<String>> getIntersectMap(List<TermQuery> l) {
        Map<String, List<String>> intersectMap = [:]
        for (tqRoot in l)
            for (tqNew in l) {
                def qti = QueryTermIntersect.getIntersectValue(tqRoot, tqNew)
                String tqRootString = tqRoot.term.text()
                String tqNewString = tqNew.term.text()
                if (qti > 0.4 && tqRootString != tqNewString) {

                    if (intersectMap.containsKey(tqRootString)) {
                        intersectMap[tqRootString].add(tqNewString)
                    } else {
                        intersectMap[tqRootString] = [tqNewString]
                    }
                }
            }
        return intersectMap
    }


    static void main(String[] args) {

        IndexEnum ie = IndexEnum.NG6
        Indexes.setIndex(ie)
        def l = Indexes.termQueryList
        //def l = ImportantTermQueries.getTFIDFTermQueryList(ie.indexReader, 100)
        IntersectWordMap iwm = new IntersectWordMap()


        def im = iwm.getIntersectMap(l)
        print ("im $im")
        im.each { rootW, valueList ->
            print "rootW: $rootW"
            valueList.each { value ->
                print " $value "
            }
            println()
        }
    }
}
