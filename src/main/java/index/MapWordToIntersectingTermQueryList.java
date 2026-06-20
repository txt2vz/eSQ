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

    public static final int INTERSECT_MAP_MAX_LENGTH = 8;

    public Map<String, List<TermQuery>> getIntersectingTerms(List<TermQuery> list, double MIN_INTERSECT_RATIO) throws IOException {
        Map<String, List<TermQuery>> orderedWordToTermQueryMap = new HashMap<>();

        for (TermQuery tqRoot : list) {
            String tqRootString = tqRoot.getTerm().text();
            Map<String, Double> wordWithRatio = new HashMap<>();

            for (TermQuery tqNew : list) {
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

    public Map<String, List<TermQuery>> getIntersectingTerms(List<TermQuery> list) throws IOException {
        return getIntersectingTerms(list, QueryTermIntersectRatio.MIN_INTERSECT_RATIO);
    }
}
