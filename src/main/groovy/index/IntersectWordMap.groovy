package index

import cluster.QueryTermIntersect
import groovy.transform.CompileStatic
import org.apache.lucene.search.TermQuery

@CompileStatic
class IntersectWordMap {

    final static int MAX_TERMQUERYLIST_SIZE = 120
    final double MIN_INTERSECT_RATIO = 0.5d

    Map<String, List<String>> getIntersectMap(List<TermQuery> l) {
        Map<String, List<String>> intersectMap = [:]
        for (tqRoot in l)
            for (tqNew in l) {
                def qti = QueryTermIntersect.getIntersectValue(tqRoot, tqNew)
                String tqRootString = tqRoot.term.text()
                String tqNewString = tqNew.term.text()
                if (qti > MIN_INTERSECT_RATIO && tqRootString != tqNewString) {

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
        Indexes.setIndex(IndexEnum.NG6)
        def l = Indexes.termQueryList
        //def l = ImportantTermQueries.getTFIDFTermQueryList(ie.indexReader, 100)
        IntersectWordMap iwm = new IntersectWordMap()

        def im = iwm.getIntersectMap(l)
        print ("im $im")
        im.each { rootW, valueList ->
            print "<$rootW>"
            valueList.each { value ->
                print " $value "
            }
            println()
        }
    }
}
