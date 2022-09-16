package index

import cluster.QueryTermIntersect
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.apache.lucene.index.*
import org.apache.lucene.search.DocIdSetIterator
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.search.similarities.TFIDFSimilarity
import org.apache.lucene.util.BytesRef

@CompileStatic
class ImportantTermQueries {

    static Set<String> stopSet = StopSet.getStopSetFromFile()
    final static int MAX_TERMQUERYLIST_SIZE = 120
    //final static int MAX_INTERSECT_LIST_SIZE = 40

    static List<TermQuery> getTFIDFTermQueryList(IndexReader indexReader, final int maxSize = MAX_TERMQUERYLIST_SIZE) {

        TermsEnum termsEnum = MultiFields.getTerms(indexReader, Indexes.FIELD_CONTENTS).iterator()

        Map<TermQuery, Double> termQueryMap = [:]
        BytesRef termbr;
        TFIDFSimilarity tfidfSim = new ClassicSimilarity()
        final int totalDocs = indexReader.numDocs()

        while ((termbr = termsEnum.next()) != null) {

            Term t = new Term(Indexes.FIELD_CONTENTS, termbr);
            final int df = indexReader.docFreq(t)
            String word = t.text()

            if (isUsefulTerm(df, word)) {

                final long docFreq = indexReader.docFreq(t);
                double tfidfTotal = 0

                PostingsEnum docsEnum = termsEnum.postings(MultiFields.getTermDocsEnum(indexReader, Indexes.FIELD_CONTENTS, termbr))
                if (docsEnum != null) {
                    while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {

                        final double tfidf = tfidfSim.tf(docsEnum.freq()) * tfidfSim.idf(totalDocs, docFreq)
                        tfidfTotal += tfidf
                    }
                }
                termQueryMap += [(new TermQuery(t)): tfidfTotal]
            }
        }

        termQueryMap = termQueryMap.sort { a, b -> a.value <=> b.value }
        List<TermQuery> tql = new ArrayList<TermQuery>(termQueryMap.keySet().take(maxSize))

        println "termQueryMap size: ${termQueryMap.size()}  termQuerylist size: ${tql.size()}  termQuerylist (first 20): ${tql.take(20)}"
        println "termQueryMap (40) ${termQueryMap.take(40)}"
        // println "termQueryMap all ${termQueryMap}"
        return tql.asImmutable()
    }

    private static boolean isUsefulTerm(int df, String word) {

        if (df < 3) return false

        if (!word.charAt(0).isLetter()) return false
        if (word.length() < 2) return false

        for (char c : word.toCharArray()){
            if (!c.isLetterOrDigit())
                return false
        }

        if (stopSet.contains(word)) return false

        return true
    }

    // for each term find and store intersecting terms
//    static Map<TermQuery, List<Tuple2<TermQuery, Double>>> getTermIntersectMapSorted(List<TermQuery> tqList) {
//
//        Map<TermQuery, List<Tuple2<TermQuery, Double>>> termIntersectMapLocal = [:]
//
//        tqList.each { TermQuery tqRoot ->
//
//            List<TermQuery> tqListMinus = tqList - tqRoot
//            List<Tuple2<TermQuery, Double>> listRelatedTuples = []
//
//            tqListMinus.each { TermQuery tqRelated ->
//
//                final double intersectValue = QueryTermIntersect.getIntersectValue(tqRoot, tqRelated)
//
//                if (intersectValue >= Indexes.MIN_INTERSECT_RATIO) {
//
//                    listRelatedTuples << new Tuple2(tqRelated, intersectValue)
//                }
//            }
//
//            listRelatedTuples.sort { -it.v2 }.take(MAX_INTERSECT_LIST_SIZE)
//            termIntersectMapLocal << [(tqRoot): listRelatedTuples]
//        }
//
//        return termIntersectMapLocal
//    }


    static void main(String[] args) {

        final Date start = new Date()

        def l = getTFIDFTermQueryList(IndexEnum.CRISIS3.indexReader)
        println " l  $l"

        final Date end = new Date()
        TimeDuration duration = TimeCategory.minus(end, start)
        println "Duration: $duration"

    }
}
