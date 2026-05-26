package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.Query

@CompileStatic
class QueryTextFile {

    static void addQueriesToFile(File qFile, Map<Query, Integer> queryMap){

            qFile.text = ''
            queryMap.keySet().each { Query q ->
                qFile << q.toString(Indexes.FIELD_CONTENTS) + '\n'
            }
        }

    static List<Query> retrieveQueryListFromFile (File qFile){
        List<Query> queries = []
        QueryParser parser = new QueryParser(Indexes.FIELD_CONTENTS, Indexes.analyzer)

        qFile.eachLine { String line ->
            Query qn = parser.parse(line)
            queries << qn
        }
        return queries
    }
}
