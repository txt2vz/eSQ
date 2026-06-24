package cluster

import io.jenetics.Genotype
import io.jenetics.IntegerChromosome
import io.jenetics.IntegerGene
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery
import org.apache.lucene.index.Term
import spock.lang.Specification
import index.Indexes

class EsqQueryBuilderTest extends Specification {

    def "constructor initializes fields correctly with default BooleanClause.Occur"() {
        given:
        def termQueryList = [new TermQuery(new Term(Indexes.FIELD_CONTENTS, "term1")), 
                             new TermQuery(new Term(Indexes.FIELD_CONTENTS, "term2")), 
                             new TermQuery(new Term(Indexes.FIELD_CONTENTS, "term3"))]
        def orderedIntersectMap = [:]
        def builderMethod = EsqQueryBuilderMethod.SINGLE
     
        when:
        def builder = new EsqQueryBuilder(termQueryList, orderedIntersectMap, builderMethod)

        then:
        builder != null
    }

    def "single word query"() {
    given:
        def termQueryList = [new TermQuery(new Term(Indexes.FIELD_CONTENTS, "term1")), 
                             new TermQuery(new Term(Indexes.FIELD_CONTENTS, "term2")), 
                             new TermQuery(new Term(Indexes.FIELD_CONTENTS, "term3"))]
        def orderedIntersectMap = [:]
        def builder = new EsqQueryBuilder(termQueryList, orderedIntersectMap, EsqQueryBuilderMethod.SINGLE)
      
        when:
        int[] chromosome = [0, 1, 2]
        int k = 3

        def result = builder.getSingleWordQueries(chromosome, k)

        then:
        result.length == 3
        result.every { it instanceof BooleanQuery.Builder }
        def firstQuery = result[0].build()
        firstQuery.clauses().size() == 1
        firstQuery.clauses()[0].query() instanceof TermQuery
        
        def tq = firstQuery.clauses()[0].query() as TermQuery
        tq.toString(Indexes.FIELD_CONTENTS) == "term1"   

    }
}
