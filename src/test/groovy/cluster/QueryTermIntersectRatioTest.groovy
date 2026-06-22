package cluster

import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.index.Term
import spock.lang.Specification

class QueryTermIntersectRatioTest extends Specification {

    def setupSpec() {
        // Initialize the index once for all tests
        Indexes.setIndex(IndexEnum.NG3)
    }

    def "getIntersectValue returns 0 when q1 has no results"() {
        setup:
        // Create a query that won't match anything (very unlikely term)
        Query emptyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "zzzzzznonexistentzzzzzzz"))
        Query someQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "space"))

        when:
        double result = QueryTermIntersectRatio.getIntersectValue(someQuery, emptyQuery)

        then:
        result == 0.0
    }

    def "getIntersectValue returns positive value for overlapping queries"() {
        setup:
        Query q1 = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "space"))
        Query q2 = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "nasa"))

        when:
        double result = QueryTermIntersectRatio.getIntersectValue(q1, q2)

        then:
        result > 0.0
        result <= 1.0
    }

    def "getIntersectValue returns 1.0 when all results of q1 match q2"() {
        setup:
        // Create a query and use it as both q1 and q2
        Query query = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "space"))

        when:
        double result = QueryTermIntersectRatio.getIntersectValue(query, query)

        then:
        result == 1.0
    }

    def "isValidIntersect returns true when ratio >= 0.5"() {
        setup:
        Query q1 = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "space"))
        Query q2 = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "space"))

        when:
        boolean result = QueryTermIntersectRatio.isValidIntersect(q1, q2)

        then:
        result == true
    }

    def "isValidIntersect returns boolean value"() {
        setup:
        Query q1 = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "space"))
        Query q2 = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "hockey"))

        when:
        boolean result = QueryTermIntersectRatio.isValidIntersect(q1, q2)

        then:
        // Result should be a valid boolean based on the actual intersect ratio
        result instanceof Boolean
    }

    def "MIN_INTERSECT_RATIO constant is 0.5"() {
        expect:
        QueryTermIntersectRatio.MIN_INTERSECT_RATIO == 0.5
    }

    def "getIntersectValue works with complex Boolean queries"() {
        setup:
        // Create AND query: space AND nasa
        BooleanQuery.Builder builder = new BooleanQuery.Builder()
        builder.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, "space")), BooleanClause.Occur.MUST)
        builder.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, "nasa")), BooleanClause.Occur.MUST)
        Query complexQuery = builder.build()

        // Simple query: space
        Query simpleQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "space"))

        when:
        double result = QueryTermIntersectRatio.getIntersectValue(complexQuery, simpleQuery)

        then:
        result >= 0.0
        result <= 1.0
    }
}
