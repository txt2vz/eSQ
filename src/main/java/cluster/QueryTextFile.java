package cluster;

import groovy.json.JsonOutput;
import index.Indexes;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryTextFile {

    public static void addQueriesToFile(File qFile, Map<Query, Integer> queryMap) throws IOException {
        Path path = qFile.toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Query q : queryMap.keySet()) {
                writer.write(q.toString(Indexes.FIELD_CONTENTS));
                writer.newLine();
            }
        }
    }

    public static List<Query> retrieveQueryListFromFile(File qFile) throws IOException, ParseException {
        List<Query> queries = new ArrayList<>();
        QueryParser parser = new QueryParser(Indexes.FIELD_CONTENTS, Indexes.analyzer);
        Path path = qFile.toPath();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Query qn = parser.parse(line);
                queries.add(qn);
            }
        }

        return queries;
    }
}
