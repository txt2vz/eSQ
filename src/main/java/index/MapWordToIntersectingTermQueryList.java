package index;

import cluster.QueryTermIntersectRatio;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapWordToIntersectingTermQueryList {

    /**
     * Builds a mapping from each query term to the top related terms whose overlap ratio
     * exceeds a threshold, so downstream code can retrieve the most intersecting terms. 
     * By default, the threshold is set to 0.5, meaning that at least half of the documents
     * retrieved by the second term must also be retrieved by the first term for it to be 
     * considered a valid intersection.
     */
    public static final int INTERSECT_MAP_MAX_LENGTH = 8;

    public Map<String, List<TermQuery>> getIntersectingTerms(List<TermQuery> termQuerylist, double MIN_INTERSECT_RATIO) throws IOException {
        Map<String, List<TermQuery>> orderedWordToTermQueryMap = new HashMap<>();

        for (TermQuery tqRoot : termQuerylist) {
            String tqRootString = tqRoot.getTerm().text();
            Map<String, Double> wordWithRatio = new HashMap<>();

            for (TermQuery tqNew : termQuerylist) {
                double qti = QueryTermIntersectRatio.getIntersectValue(tqRoot, tqNew);
                String tqNewString = tqNew.getTerm().text();
                if (qti > MIN_INTERSECT_RATIO && !tqRootString.equals(tqNewString)) {
                    wordWithRatio.put(tqNewString, qti);
                }
            }

            if (!wordWithRatio.isEmpty()) {
                List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(wordWithRatio.entrySet());
                sortedEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

                List<TermQuery> orderedTermQueryList = new ArrayList<>();
                int limit = Math.min(sortedEntries.size(), INTERSECT_MAP_MAX_LENGTH);
                for (int i = 0; i < limit; i++) {
                    orderedTermQueryList.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, sortedEntries.get(i).getKey())));
                }

                if (!orderedTermQueryList.isEmpty()) {
                    orderedWordToTermQueryMap.put(tqRootString, orderedTermQueryList);
                }
            }
        }

        return orderedWordToTermQueryMap;
    }

    public Map<String, List<TermQuery>> getIntersectingTerms(List<TermQuery> termQuerylist) throws IOException {
        return getIntersectingTerms(termQuerylist, QueryTermIntersectRatio.MIN_INTERSECT_RATIO);
    }
}
