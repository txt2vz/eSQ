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

        when:
        def firstQuery = result[0].build()

        then:
        firstQuery.clauses().size() == 1
        firstQuery.clauses()[0].query() instanceof TermQuery
        
        when:
        def tq = firstQuery.clauses()[0].query() as TermQuery

        then:
        tq.toString(Indexes.FIELD_CONTENTS) == "term1"   

    }

    def "multi word intersect query"() {
        given:
        def termQueryList = [new TermQuery(new Term(Indexes.FIELD_CONTENTS, "term0")), 
                             new TermQuery(new Term(Indexes.FIELD_CONTENTS, "term1")), 
                             new TermQuery(new Term(Indexes.FIELD_CONTENTS, "term2"))]
       
        def intersectTermQueryList = [new TermQuery(new Term(Indexes.FIELD_CONTENTS, "Intersect term0")), 
                             new TermQuery(new Term(Indexes.FIELD_CONTENTS, "Intersect term1")), 
                             new TermQuery(new Term(Indexes.FIELD_CONTENTS, "Intersect term2"))]

        def orderedIntersectMap = ["term0": intersectTermQueryList] 
        def builder = new EsqQueryBuilder(termQueryList, orderedIntersectMap, EsqQueryBuilderMethod.INTERSECT)
      
        when:
        int k = 3
        int[] rootChromosome = [0, 1, 2]
        int[] intersectChromosome = [0, 1, 2]
        def listOfBuilders = builder.getIntersectQueries(rootChromosome, intersectChromosome, k)

        then:
        listOfBuilders.length == k
        listOfBuilders.every { it instanceof BooleanQuery.Builder }

        when:
        def query0 = listOfBuilders[0].build()

        then:
        query0.clauses().size() == 4  //root term + 3 intersect terms
        query0.clauses()[0].query() instanceof TermQuery
        println("query0: ${query0.toString()}")
        
        when:
        def tq0 = query0.clauses()[0].query() as TermQuery
        def tq1 = query0.clauses()[1].query() as TermQuery
        def tq2 = query0.clauses()[2].query() as TermQuery
        def tq3 = query0.clauses()[3].query() as TermQuery

        then:
        tq0.toString(Indexes.FIELD_CONTENTS) == "term0"   
        tq1.toString(Indexes.FIELD_CONTENTS) == "Intersect term0"
        tq2.toString(Indexes.FIELD_CONTENTS) == "Intersect term1"
        tq3.toString(Indexes.FIELD_CONTENTS) == "Intersect term2"

        when:
        //repeated 2 in chromosome 
        intersectChromosome = [0, 2, 2]
        listOfBuilders = builder.getIntersectQueries(rootChromosome, intersectChromosome, k)
        query0 = listOfBuilders[0].build()

        then:
        query0.clauses().size() == 3   // only 3 clauses because the second intersect term is repeated
        query0.clauses()[0].query() instanceof TermQuery

        when:
        tq0 = query0.clauses()[0].query() as TermQuery
        tq1 = query0.clauses()[1].query() as TermQuery  
        tq2 = query0.clauses()[2].query() as TermQuery

        then:
        tq0.toString(Indexes.FIELD_CONTENTS) == "term0"
        tq1.toString(Indexes.FIELD_CONTENTS) == "Intersect term0"
        tq2.toString(Indexes.FIELD_CONTENTS) == "Intersect term2" //term1 is not included  - no 1 in chromosome.

    }
}
