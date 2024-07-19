package index

import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.CompileStatic
import org.apache.lucene.index.*
import org.apache.lucene.search.DocIdSetIterator
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.util.BytesRef

@CompileStatic
class ImportantTermQueries {

    static Set<String> stopSet = StopSet.getStopSetFromFile()
    final static int MAX_TERMQUERYLIST_SIZE = 120

    static List<TermQuery> getTFIDFTermQueryList(IndexReader indexReader, final int maxSize = MAX_TERMQUERYLIST_SIZE) {

        Map<TermQuery, Double> termQueryMap = [:]
        int totalDocs = indexReader.numDocs()

        for (LeafReaderContext context : indexReader.leaves()) {
            LeafReader leafReader = context.reader();
            Terms terms = leafReader.terms(Indexes.FIELD_CONTENTS);
            if (terms == null) continue;

            TermsEnum termsEnum = terms.iterator();
            BytesRef term;

            while ((term = termsEnum.next()) != null) {

                Term t = new Term(Indexes.FIELD_CONTENTS, term)
                long docFreq = indexReader.docFreq(t)

                if (isUsefulTerm(docFreq, t.text())) {

                    assert docFreq > 0
                    assert totalDocs > 0

                    double idf = Math.log((double) (totalDocs) / (double) (docFreq))  + 1.0

                    double tfidfTotal = 0
                    double tfidf
                    PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);

                    while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {

                        int termFreqInCurrentDoc = postingsEnum.freq();

                        //double tf = Math.sqrt((double) termFreqInCurrentDoc)
                        tfidf = termFreqInCurrentDoc * (2 - idf)
                        tfidfTotal += tfidf
                    }

                    termQueryMap += [(new TermQuery(t)): tfidfTotal]
                }
            }
        }

        //Descending order
        def termQueryMapDescend = termQueryMap.sort { a, b -> a.value <=> b.value }

        def tql = new ArrayList<TermQuery>(termQueryMapDescend.keySet().take(maxSize))
        println "termQueryMapDescend first (40): ${termQueryMapDescend.take(40)}"
        println "termQueryList first (40): ${tql.take(40)}"

        List<TermQuery> tql40 = tql.take(40)
        print "Important words: "
        tql40.each { Query q ->
            print " ${q.toString(Indexes.FIELD_CONTENTS)}"
        }
        println ""

        return tql.asImmutable()
    }

    private static boolean isUsefulTerm(long df, String word) {

        if (df < 3) return false
        if (!word.charAt(0).isLetter()) return false
        if (word.length() < 2) return false

        for (char c : word.toCharArray()) {
            if (!c.isLetterOrDigit())
                return false
        }

        if (stopSet.contains(word)) return false

        return true
    }

    static void main(String[] args) {

        final Date start = new Date()

        def l = getTFIDFTermQueryList(IndexEnum.NG3.indexReader)

        final Date end = new Date()
        TimeDuration duration = TimeCategory.minus(end, start)
        println "Duration: $duration"
    }
}
