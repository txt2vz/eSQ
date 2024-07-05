package index


import groovy.time.TimeCategory
import groovy.time.TimeDuration
import org.apache.lucene.index.*
import org.apache.lucene.search.DocIdSetIterator
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.search.similarities.TFIDFSimilarity
import org.apache.lucene.util.BytesRef


import groovy.transform.CompileStatic

@CompileStatic
class ImportantTermQueries {

    static Set<String> stopSet = StopSet.getStopSetFromFile()
    final static int MAX_TERMQUERYLIST_SIZE = 120
    private org.apache.lucene.util.BytesRef termbr

    static List<TermQuery> getTFIDFTermQueryList(IndexReader indexReader, final int maxSize = MAX_TERMQUERYLIST_SIZE) {

        Map<TermQuery, Double> termQueryMap = [:]

        TFIDFSimilarity tfidfSim = new ClassicSimilarity()

        for (LeafReaderContext context : indexReader.leaves()) {
            LeafReader leafReader = context.reader();
            Terms terms = leafReader.terms(Indexes.FIELD_CONTENTS);
            if (terms == null) continue;

            TermsEnum termsEnum = terms.iterator();
            BytesRef term;

            while ((term = termsEnum.next()) != null) {

                Term t = new Term(Indexes.FIELD_CONTENTS, term);
                long docFreq = indexReader.docFreq(t);

                if (isUsefulTerm(docFreq, t.text())) {
                    double tfidfTotal = 0
                    PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);
                    while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {


                        // Get term frequency for the current document
                        int termFreqInCurrentDoc = postingsEnum.freq();

                        // a puzzle -  the parameters for idf the wrong way round but it works?
                        final double tfidf = tfidfSim.tf(termFreqInCurrentDoc) * tfidfSim.idf(indexReader.numDocs(), docFreq)

                        tfidfTotal += tfidf
                    }
                    termQueryMap += [(new TermQuery(t)): tfidfTotal]
                }
            }
        }

        termQueryMap = termQueryMap.sort { a, b -> a.value <=> b.value }
        List<TermQuery> tql = new ArrayList<TermQuery>(termQueryMap.keySet().take(maxSize))

        println "After sort termQueryMap size: ${termQueryMap.size()}  termQuerylist size: ${tql.size()}  termQuerylist (first 40): ${tql.take(40)}"
        println "termQueryMap (40) ${termQueryMap.take(40)}"
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
        println "Important word list:  $l"

        final Date end = new Date()
        TimeDuration duration = TimeCategory.minus(end, start)
        println "Duration: $duration"
    }
}
