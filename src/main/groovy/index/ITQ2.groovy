package index

import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.CompileStatic
import org.apache.lucene.index.*
import org.apache.lucene.search.DocIdSetIterator
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.search.similarities.TFIDFSimilarity
import org.apache.lucene.util.BytesRef

@CompileStatic
class ITQ2 {

    static Set<String> stopSet = StopSet.getStopSetFromFile()
    final static int MAX_TERMQUERYLIST_SIZE = 120

    static List<TermQuery> getTFIDFTermQueryList(IndexReader indexReader, final int maxSize = MAX_TERMQUERYLIST_SIZE, boolean useDocFreqOverDocTotal = true) {

        Map<TermQuery, Double> termQueryMap = [:]

        TFIDFSimilarity tfidfSim = new ClassicSimilarity()
        int totalDocs = indexReader.numDocs()

        for (LeafReaderContext context : indexReader.leaves()) {
            LeafReader leafReader = context.reader();
            Terms terms = leafReader.terms(Indexes.FIELD_CONTENTS);
            if (terms == null) continue;

            TermsEnum termsEnum = terms.iterator();
            BytesRef term;

            while ((term = termsEnum.next()) != null) {

                Term t = new Term(Indexes.FIELD_CONTENTS, term);
                long docFreq = indexReader.docFreq(t);
                double idfSim=0;
                double idfSimLog;
                double idf;

                if (useDocFreqOverDocTotal) {
                    idfSim = (double) (docFreq + 1L) / (double) (totalDocs + 1L) ;
                    idfSimLog = Math.log(idfSim) + 1.0 ;

                } else {
                    idfSim = (double) (docFreq + 1L) / (double) (totalDocs + 1L) + 1.0;
                    idfSimLog = Math.log(idfSim ) + 1.0;

                }

                if (isUsefulTerm(docFreq, t.text())) {
                    double tfidfTotal = 0
                    PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);
                    while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {

                        // Get term frequency for the current document
                        int termFreqInCurrentDoc = postingsEnum.freq();
                        int docID = postingsEnum.docID();

                        // a puzzle -  the parameters for idf the wrong way round but it works?
                      //  final double tfidf = tfidfSim.tf(termFreqInCurrentDoc) * tfidfSim.idf(indexReader.numDocs(), docFreq)
                        double tfSim = Math.sqrt((double) termFreqInCurrentDoc); //tfidfSim.tf(termFreqInCurrentDoc)

                      //  double docFreqOverTotalDocs = docFreq / totalDocs
                       // idfSim = Math.log((double) (docFreq + 1L) / (double) (totalDocs + 1L)) + 1.0;



                     //idfSim =  Math.log( (double)(totalDocs + 1L) / (double)(docFreq + 1L)) + 1.0;
                      //  idfSim = Math.log((double) (docFreq + 1L) / (double) (totalDocs + 1L)) + 1.0;


                     //  double idfSim = tfidfSim.idf(indexReader.numDocs(), docFreq)
                      //  double idfSim = tfidfSim.idf(5, 100)
                       // final double tfidf = tfidfSim.tf(termFreqI
                        // nCurrentDoc) * tfidfSim.idf(docFreq,indexReader.numDocs())
                        double tfidf = tfSim * idfSimLog
                    //     tfidf = tfSim * idfSim
                     //   double tfidf = termFreqInCurrentDoc * idfSim
                        //tfidf = termFreqInCurrentDoc * idfSim

                        //   final double tfidf = tfidfSim.tf(freq) * tfidfSim.idf(indexReader.numDocs(), docFreq)
                        // final double tfidf = tfidfSim.tf(termFreqinCurrentDoc) * tfidfSim.idf(docFreq, indexReader.numDocs())
                        //final double tfidf = tfidfSim.tf(termFreqInCurrentDoc) * tfidfSim.idf(indexReader.numDocs(), docFreq)

                        // Output document ID and term frequency
                           if (t.text() == 'christ') {
                            // println("DocID: $docID tfidftotal $tfidfTotal   Term Frequency in current doc: $termFreqInCurrentDoc  docFreq $docFreq  Term  ${t.text()}  tfidf $tfidf");
                            println("DocID: $docID  tf: ${termFreqInCurrentDoc}  tfsim: ${tfSim}  tfidftotal $tfidfTotal  idfSim: $idfSim  idfsimlog: ${idfSimLog} docFreq $docFreq Term  ${t.text()}  tfidf $tfidf");
                        }



                        tfidfTotal += tfidf
                    }
                    termQueryMap += [(new TermQuery(t)): tfidfTotal]
                }
            }
        }


        //Descending order
        def termQueryMapDescend = termQueryMap.sort { a, b -> a.value <=> b.value }

      //ascending order
        def termQueryMapAsc = termQueryMap.sort { a, b -> b.value <=> a.value }

        def tqlDescend = new ArrayList<TermQuery>(termQueryMapDescend.keySet().take(maxSize))
        def tqlAscend = new ArrayList<TermQuery>(termQueryMapAsc.keySet().take(maxSize))

     //   println "After Next sort termQueryMapDescend size: ${termQueryMapDescend.size()}  termQuerylist size: ${tql.size()}  termQuerylist (first 40): ${tql.take(40)}"

        println "termQueryMapDescend first after (40) ${termQueryMapDescend.take(40)}"
        println "termQueryMapAsc first after (40) ${termQueryMapAsc.take(40)}"

       // println "termQueryList last r (40) ${tql.takeRight(40) }"

//        List<TermQuery> tql40 = tql.take(40)
//        print "Important words: "
//        tql40.each { Query q ->
//           print  " ${q.toString(Indexes.FIELD_CONTENTS) }"
//        }
//        println ""


        return tqlAscend.asImmutable()
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

      //  def l = getTFIDFTermQueryList(IndexEnum.space.indexReader, 120, true)    //NG3.indexReader)
        def l = getTFIDFTermQueryList(IndexEnum.NG3.indexReader)
       // println "Important word list:  $l"

        final Date end = new Date()
        TimeDuration duration = TimeCategory.minus(end, start)
        println "Duration: $duration"
    }
}
