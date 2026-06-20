package index;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportantTermQueries {

    public static final int MAX_TERMQUERYLIST_SIZE = 120;

    public static List<TermQuery> getTFIDFTermQueryList(IndexReader indexReader, int maxSize) throws IOException {
        Map<TermQuery, Double> termQueryMap = new HashMap<>();
        int totalDocs = indexReader.numDocs();

        for (LeafReaderContext context : indexReader.leaves()) {
            LeafReader leafReader = context.reader();
            Terms terms = leafReader.terms(Indexes.FIELD_CONTENTS);
            if (terms == null) {
                continue;
            }

            TermsEnum termsEnum = terms.iterator();
            BytesRef term;
            while ((term = termsEnum.next()) != null) {
                Term t = new Term(Indexes.FIELD_CONTENTS, term);
                long docFreq = indexReader.docFreq(t);

                if (isUsefulTerm(docFreq, t.text(), indexReader)) {
                    double idf = Math.log((double) totalDocs / (double) docFreq) + 1.0;
                    double tfidfTotal = 0.0;
                    PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);

                    while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                        int termFreqInCurrentDoc = postingsEnum.freq();
                        double tf = Math.sqrt((double) termFreqInCurrentDoc);
                        double tfidf = tf * (2.0 - idf);
                        tfidfTotal += tfidf;
                    }

                    termQueryMap.put(new TermQuery(t), tfidfTotal);
                }
            }
        }

        List<Map.Entry<TermQuery, Double>> sortedEntries = new ArrayList<>(termQueryMap.entrySet());
        sortedEntries.sort(Comparator.comparing(Map.Entry<TermQuery, Double>::getValue));

        List<TermQuery> tql = new ArrayList<>();
        for (int i = 0; i < sortedEntries.size() && i < maxSize; i++) {
            tql.add(sortedEntries.get(i).getKey());
        }

        System.out.print("Important words:");
        int previewSize = Math.min(tql.size(), 40);
        for (int i = 0; i < previewSize; i++) {
            Query q = tql.get(i);
            System.out.print(" " + q.toString(Indexes.FIELD_CONTENTS));
        }
        System.out.println();
        System.out.println("TermQueryList size: " + tql.size());

        return Collections.unmodifiableList(tql);
    }

    private static boolean isUsefulTerm(long df, String word, IndexReader indexReader) throws IOException {
        if (word.length() < 2) {
            return false;
        }
        if (df < 4) {
            return false;
        }
        if (df > 0.5 * indexReader.numDocs()) {
            System.out.println("Term " + word + " has df " + df + " which is more than 50% of total docs " + indexReader.numDocs() + ". Ignoring it.");
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        Instant start = Instant.now();
        IndexEnum indexEnum = IndexEnum.NG3;
        Indexes.setIndex(indexEnum);

        List<TermQuery> l = getTFIDFTermQueryList(indexEnum.getIndexReader(), MAX_TERMQUERYLIST_SIZE);

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println("Duration: " + duration);
    }
}
