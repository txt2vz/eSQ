package index

import cluster.QueryTermIntersect
import groovy.transform.CompileStatic
import org.apache.lucene.search.TermQuery

//@CompileStatic
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

        Indexes.setIndex(IndexEnum.NG5)
        Indexes.setImportantTermQueryList()
        def l = Indexes.termQueryList
        //def l = ImportantTermQueries.getTFIDFTermQueryList(ie.indexReader, 100)
        IntersectWordMap iwm = new IntersectWordMap()

        def im = iwm.getIntersectMap(l)
        print("im $im")
        im.each { rootW, valueList ->
            print "<$rootW>"
            valueList.each { value ->
                print " $value "
            }
            println()
        }

        List<Set<String>>  input = [
                ['apple', 'banana'] as Set,
                ['cherry', 'date'] as Set,
                ['banana', 'cherry'] as Set, // Overlaps with the first two
                ['fig', 'grape'] as Set,
                ['grape', 'honeydew'] as Set
        ]


        def result =  iwm.mergeSets(input)

// Display results
        result.eachWithIndex { set, i ->
            println "Group ${i + 1}: ${set}"
        }
    }
}

