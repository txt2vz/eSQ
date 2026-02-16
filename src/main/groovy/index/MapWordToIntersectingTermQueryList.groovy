package index

import cluster.QueryTermIntersectRatio
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

//@CompileStatic
class MapWordToIntersectingTermQueryList {

    final static int MAX_TERMQUERYLIST_SIZE = 120
    final static double MIN_INTERSECT_RATIO = QueryTermIntersectRatio.MIN_INTERSECT_RATIO
    final static int INTERSECT_MAP_MAX_LENGTH = 8

    static Map<String, List<TermQuery>> getIntersectingTerms(List<TermQuery> l) {

        Map<String, List<TermQuery>> orderedWordToTermQueryMap = [:]

        for (tqRoot in l) {

            String tqRootString = tqRoot.term.text()

            //should not use TermQuery as key to a Map
            Map<String, Double> wordWithRatio = [:]
            for (tqNew in l) {

                double qti = QueryTermIntersectRatio.getIntersectValue(tqRoot, tqNew)
                String tqNewString = tqNew.term.text()

                if (qti > MIN_INTERSECT_RATIO && tqRootString != tqNewString) {
                    wordWithRatio[tqNewString] = qti
                }
            }

            if (wordWithRatio.size() > 0) {

                Map<String, Double> topWords = wordWithRatio.sort { a, b -> b.value <=> a.value }.take(INTERSECT_MAP_MAX_LENGTH)
                // println "rootword tqRootString: $tqRootString topWords: $topWords"

                List<String> orderedWordList = topWords.keySet().asList()
                List<TermQuery> orderedTermQueryList = []
                for (word in orderedWordList) {
                    TermQuery tq = new TermQuery(new Term(Indexes.FIELD_CONTENTS, word))
                    orderedTermQueryList.add(tq)
                }

                if (orderedWordList.size() > 0) {
                    orderedWordToTermQueryMap[tqRootString] = orderedTermQueryList
                }
            }
        }
        println "sorted orderedWordToTermQueryMap: $orderedWordToTermQueryMap"
        return orderedWordToTermQueryMap
    }

    static void main(String[] args) {

        Indexes.setIndex(IndexEnum.NG3)
        Indexes.setImportantTermQueryList()

        MapWordToIntersectingTermQueryList wtitm = new MapWordToIntersectingTermQueryList()

        Map<String, List<TermQuery>> orderedIntersectMap = wtitm.getIntersectingTerms(Indexes.termQueryList)

        orderedIntersectMap.each { String rootW, List<TermQuery> tql ->
            print "<$rootW> "
            tql.each { TermQuery q ->
                print "${q.toString(Indexes.FIELD_CONTENTS)} "
            }
            println ""
        }

        println ""
        println "Map with ordered list:  $orderedIntersectMap"
    }
}

