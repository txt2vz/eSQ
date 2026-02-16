package index

import cluster.QueryTermIntersectRatio
import groovy.transform.Immutable
import org.apache.lucene.search.TermQuery

//@CompileStatic
class IntersectWordMap {

    final static int MAX_TERMQUERYLIST_SIZE = 120
    final static double MIN_INTERSECT_RATIO = QueryTermIntersectRatio.MIN_INTERSECT_RATIO

    Map<String, List<String>> getIntersectMap(List<TermQuery> l) {
        Map<String, List<String>> intersectMap = [:]
        for (tqRoot in l)
            for (tqNew in l) {
                def qti = QueryTermIntersectRatio.getIntersectValue(tqRoot, tqNew)
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

    List<Set<String>> getListOfWordSets(List<TermQuery> l) {

        List<Set<String>> listOfWordSets = []
        for (tqRoot in l) {

            Set setOfWords = [] as Set
            String tqRootString = tqRoot.term.text()
            setOfWords << tqRootString
            for (tqNew in l) {

                def qti = QueryTermIntersectRatio.getIntersectValue(tqRoot, tqNew)
                String tqNewString = tqNew.term.text()

                if (qti > MIN_INTERSECT_RATIO && tqRootString != tqNewString) {
                    setOfWords << tqNewString
                }
            }

            if (setOfWords.size() > 2)
                listOfWordSets << setOfWords
        }
        return listOfWordSets
    }

    static Map<String, List<String>> getOrderedIntersectMap(List<TermQuery> l, double M) {

        Map<String, List<String>> orderedWordMap = [:]

        for (tqRoot in l) {

            String tqRootString = tqRoot.term.text()

            Map wordWithRatio = [:]
            for (tqNew in l) {

                double qti = QueryTermIntersectRatio.getIntersectValue(tqRoot, tqNew)
                String tqNewString = tqNew.term.text()

                if (qti > MIN_INTERSECT_RATIO && tqRootString != tqNewString) {
                    wordWithRatio[tqNewString] = qti
                }
            }

            def topWords = wordWithRatio.sort { a, b -> b.value <=> a.value }.take(8)
            List <String> orderedWordList = topWords.keySet() as List
            if (orderedWordList.size()>0) {
                orderedWordMap[tqRootString] = orderedWordList
            }
        }
       // println "sorted orderedWordMap: $orderedWordMap"
        return orderedWordMap
    }

    def mergeSets(List<Set<String>> source) {
        def merged = []

        source.each { currentSet ->
            // Find all sets in our 'merged' list that share elements with the current set
            def (overlapping, distinct) = merged.split { existingSet ->
                !existingSet.disjoint(currentSet)
            }

            // Combine the current set with all overlapping sets found so far
            def newMergedSet = overlapping.inject(currentSet) { acc, set -> acc + set }

            // Update our list: keep the distinct ones and add the newly expanded set
            merged = distinct + [newMergedSet]
        }

        return merged
    }


    static void main(String[] args) {

        Indexes.setIndex(IndexEnum.NG3)
        Indexes.setImportantTermQueryList()
        def termQueryList = Indexes.termQueryList

        IntersectWordMap iwm = new IntersectWordMap()
//
//        def im = iwm.getIntersectMap(termQueryList)
//        print("im: $im")
//        im.each { rootW, valueList ->
//            print "<$rootW>"
//            valueList.each { value ->
//                print " $value "
//            }
//            println()
//        }

       // def ilWithOrder = iwm.getListOfWordSetsUsingRatioOrder(termQueryList)
        Map<String, List<String>> orderedIntersectMap = iwm.getOrderedIntersectMap(termQueryList) //as Immutable

        orderedIntersectMap.each {rootW, l ->
            println "<$rootW> $l"
        }

//        for (entry in orderedIntersectMap) {
//            println "Key: ${entry.key}, Value: ${entry.value}"
//        }

//        orderedIntersectMap.each { key, value ->
//            println "Key: $key | Value: $value"
//        }



        println "Map with ordered list:  $orderedIntersectMap"

        def il = iwm.getListOfWordSets(termQueryList)
       // println "il $il"

        def result = iwm.mergeSets(il)  //input)

// Display results
        result.eachWithIndex { set, i ->
            println "Merged Group ${i + 1}: ${set}"
        }
    }
}

