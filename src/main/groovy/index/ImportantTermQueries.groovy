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
       // BytesRef termbr;
        TFIDFSimilarity tfidfSim = new ClassicSimilarity()
        final int numDocs = indexReader.numDocs()

        for (LeafReaderContext context : indexReader.leaves()) {
            LeafReader leafReader = context.reader();
            Terms terms = leafReader.terms(Indexes.FIELD_CONTENTS);
            if (terms == null) continue;

            TermsEnum termsEnum = terms.iterator();
            BytesRef term;

            while ((term = termsEnum.next()) != null) {

                Term t = new Term(Indexes.FIELD_CONTENTS, term);
                long docFreq = indexReader.docFreq(t);
              //  long totalTermFreq = indexReader.totalTermFreq(t);
              //  double idf = Math.log(numDocs / (double) (docFreq + 1)) + 1.0;
               // double tf = totalTermFreq / (double) numDocs;

                if (isUsefulTerm(docFreq, t.text())) {
                    double tfidfTotal = 0
                    PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);
                    while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                        // Get document ID
                        int docID = postingsEnum.docID();

                        // Get term frequency for the current document
                        int termFreqinCurrentDoc = postingsEnum.freq();

                        //   final double tfidf = tfidfSim.tf(freq) * tfidfSim.idf(indexReader.numDocs(), docFreq)
                        // final double tfidf = tfidfSim.tf(termFreqinCurrentDoc) * tfidfSim.idf(docFreq, indexReader.numDocs())
                        final double tfidf = tfidfSim.tf(termFreqinCurrentDoc) * tfidfSim.idf(indexReader.numDocs(), docFreq)

                        // Output document ID and term frequency
                     //   if (t.text() == 'christ')
                       //     println("DocID: $docID tfidftotal $tfidfTotal   Term Frequency in current doc: $termFreqinCurrentDoc  docFreq $docFreq  Term  ${t.text()}  tfidf $tfidf");

                        tfidfTotal += tfidf
                    }
                    termQueryMap += [(new TermQuery(t)): tfidfTotal]
                }
            }
        }

        println " Before sort termQueryMap size: ${termQueryMap.size()} "

        println "termQueryMap (40) ${termQueryMap.take(40)}"

        termQueryMap = termQueryMap.sort { a, b -> a.value <=> b.value }
        List<TermQuery> tql = new ArrayList<TermQuery>(termQueryMap.keySet().take(maxSize))

        println "After sort termQueryMap size: ${termQueryMap.size()}  termQuerylist size: ${tql.size()}  termQuerylist (first 80): ${tql.take(80)}"
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

        def l = getTFIDFTermQueryList(IndexEnum.CRISIS3.indexReader)
        println "Important word list:  $l"

        final Date end = new Date()
        TimeDuration duration = TimeCategory.minus(end, start)
        println "Duration: $duration"
    }
}
