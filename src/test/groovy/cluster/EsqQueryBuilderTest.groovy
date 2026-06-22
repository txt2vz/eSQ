package cluster

import io.jenetics.Genotype
import io.jenetics.IntegerChromosome
import io.jenetics.IntegerGene
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery
import org.apache.lucene.index.Term
import spock.lang.Specification

class EsqQueryBuilderTest extends Specification {

    def "constructor initializes fields correctly with default BooleanClause.Occur"() {
        given:
        def termQueryList = [new TermQuery(new Term("field", "term1")), 
                             new TermQuery(new Term("field", "term2")), 
                             new TermQuery(new Term("field", "term3"))]
        def orderedIntersectMap = [:]
        def builderMethod = EsqQueryBuilderMethod.SINGLE

        when:
        def builder = new EsqQueryBuilder(termQueryList, orderedIntersectMap, builderMethod)

        then:
        builder != null
    }

    def "constructor initializes fields correctly with custom BooleanClause.Occur"() {
        given:
        def termQueryList = [new TermQuery(new Term("field", "term1")), 
                             new TermQuery(new Term("field", "term2"))]
        def orderedIntersectMap = [:]
        def builderMethod = EsqQueryBuilderMethod.SINGLE
        def occur = BooleanClause.Occur.MUST

        when:
        def builder = new EsqQueryBuilder(termQueryList, orderedIntersectMap, builderMethod, occur)

        then:
        builder != null
    }

    def "getSingleWordQueries creates query builder for each allele"() {
        given:
        def termQueryList = [new TermQuery(new Term("field", "term1")), 
                             new TermQuery(new Term("field", "term2")), 
                             new TermQuery(new Term("field", "term3"))]
        def orderedIntersectMap = [:]
        def builder = new EsqQueryBuilder(termQueryList, orderedIntersectMap, EsqQueryBuilderMethod.SINGLE)
        int[] chromosome = [0, 1, 2]
        int k = 3

        when:
        def result = builder.getSingleWordQueries(chromosome, k)

        then:
        result.length == 3
        result.every { it instanceof BooleanQuery.Builder }
    }

    def "getIntersectQueries with multiple root terms"() {
        given:
        def rootTermQuery1 = new TermQuery(new Term("field", "root1"))
        def rootTermQuery2 = new TermQuery(new Term("field", "root2"))
        def intersectTermQuery = new TermQuery(new Term("field", "intersect"))
        def termQueryList = [rootTermQuery1, rootTermQuery2, intersectTermQuery]
        def orderedIntersectMap = ['root1': [intersectTermQuery], 'root2': [intersectTermQuery]]
        def builder = new EsqQueryBuilder(termQueryList, orderedIntersectMap, EsqQueryBuilderMethod.INTERSECT)
        int[] rootChromosome = [0, 1]
        int[] intersectChromosome = [0, 0, 0, 0]
        int k = 2

        when:
        def result = builder.getIntersectQueries(rootChromosome, intersectChromosome, k)
        println("Result: ${result}")

        then:
        result.length == 2
        result.every { it instanceof BooleanQuery.Builder }
    }

    def "getSingleWordQueries returns builders with SHOULD clause by default"() {
        given:
        def termQueryList = [new TermQuery(new Term("field", "term1"))]
        def orderedIntersectMap = [:]
        def builder = new EsqQueryBuilder(termQueryList, orderedIntersectMap, EsqQueryBuilderMethod.SINGLE)
        int[] chromosome = [0]
        int k = 1

        when:
        def result = builder.getSingleWordQueries(chromosome, k)

        then:
        result.length == 1
        reslt[0] instanceof BooleanQuery.Builder
    }


}
