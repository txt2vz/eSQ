package cluster;

import groovy.json.JsonOutput;
import index.Indexes;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TotalHitCountCollector;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuerySet {

    private final Map<Query, Integer> queryMap;
    private final Query[] queryArray;
    private final List<List<String>> queryTermLists = new ArrayList<>();
    private int totalHitsReturnedByOnlyOneQuery;
    private int totalHitsAllQueries;
    private final Query[] nonIntersectingQueries;

    public static final int MIN_DISTINCT_HITS = 5;

    public QuerySet(BooleanQuery.Builder[] arrayOfQueryBuilders) {
        queryMap = new HashMap<>();
        queryArray = new Query[arrayOfQueryBuilders.length];
        nonIntersectingQueries = new Query[arrayOfQueryBuilders.length];
        totalHitsAllQueries = 0;
        totalHitsReturnedByOnlyOneQuery = 0;

        BooleanQuery.Builder totalHitsBQB = new BooleanQuery.Builder();

        for (int i = 0; i < arrayOfQueryBuilders.length; i++) {
            BooleanQuery booleanQuery = (BooleanQuery) arrayOfQueryBuilders[i].build();
            queryArray[i] = booleanQuery;

            List<String> terms = new ArrayList<>();
            for (BooleanClause clause : booleanQuery.clauses()) {
                terms.add(((TermQuery) clause.query()).getTerm().text());
            }
            queryTermLists.add(terms);

            totalHitsBQB.add(booleanQuery, BooleanClause.Occur.SHOULD);

            BooleanQuery.Builder builderForNonIntersectingQuery = new BooleanQuery.Builder();
            builderForNonIntersectingQuery.add(booleanQuery, BooleanClause.Occur.SHOULD);
            for (int j = 0; j < arrayOfQueryBuilders.length; j++) {
                if (j != i) {
                    builderForNonIntersectingQuery.add(arrayOfQueryBuilders[j].build(), BooleanClause.Occur.MUST_NOT);
                }
            }

            TotalHitCountCollector distinctHitCollector = new TotalHitCountCollector();
            nonIntersectingQueries[i] = builderForNonIntersectingQuery.build();
            try {
                Indexes.indexSearcher.search(nonIntersectingQueries[i], distinctHitCollector);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            int qDistinctHits = distinctHitCollector.getTotalHits();
            if (qDistinctHits > MIN_DISTINCT_HITS) {
                queryMap.put(booleanQuery, qDistinctHits);
                totalHitsReturnedByOnlyOneQuery += qDistinctHits;
            }
        }

        TotalHitCountCollector collector = new TotalHitCountCollector();
        try {
            Indexes.indexSearcher.search(totalHitsBQB.build(), collector);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        totalHitsAllQueries = collector.getTotalHits();
    }

    public void printQueryMap() {
        for (Query q : queryMap.keySet()) {
            System.out.println("Query: " + q.toString(Indexes.FIELD_CONTENTS) + " Distinct hits: " + queryMap.get(q));
        }
    }

    public void printQueryTermLists() {
        for (int i = 0; i < queryTermLists.size(); i++) {
            System.out.println("Query terms " + i + ": " + queryTermLists.get(i));
        }
    }

    public void writeQueryTermsJson(File file) throws IOException {
        String json = JsonOutput.prettyPrint(JsonOutput.toJson(queryTermLists));
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
    }

    public List<List<String>> getQueryTermLists() {
        return queryTermLists;
    }

    public int getTotalHitsReturnedByOnlyOneQuery() {
        return totalHitsReturnedByOnlyOneQuery;
    }

    public int getTotalHitsAllQueries() {
        return totalHitsAllQueries;
    }
}
