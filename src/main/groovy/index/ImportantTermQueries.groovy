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


    static void main(String[] args) {

        final Date start = new Date()

        def l = getTFIDFTermQueryList(IndexEnum.CRISIS3.indexReader)
        println " l  $l"

        final Date end = new Date()
        TimeDuration duration = TimeCategory.minus(end, start)
        println "Duration: $duration"

    }
}
