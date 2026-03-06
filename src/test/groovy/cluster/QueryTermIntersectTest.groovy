package cluster

import index.IndexEnum
import index.Indexes
import io.jenetics.Genotype
import io.jenetics.IntegerChromosome
import io.jenetics.IntegerGene
import io.jenetics.util.Factory
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import spock.lang.Specification

class QueryTermIntersectTest extends Specification {

    def "QueryListFromChromosome single word queries NG3 tfidf"() {
        setup:
        Indexes.setIndex(IndexEnum.NG3)
        Indexes.setImportantTermQueryList()

        final Factory<Genotype<IntegerGene>> gtf = Genotype.of(
                IntegerChromosome.of(JeneticsMain.minK, JeneticsMain.maxK, 1),  //possible values of k
                IntegerChromosome.of(0, JeneticsMain.maxWordListValue, JeneticsMain.maxK), //rootword
                IntegerChromosome.of(-1, JeneticsMain.maxIntersectListSize, JeneticsMain.maxIntersectListSize * JeneticsMain.maxK)//intersect words -1 indicates no word added to query
        );

        int k = 3
        int[] rootGenome3 = new int[]{0, 1, 2}
        BuilderMethod builderMethod = BuilderMethod.INTERSECT
        EsqQueryBuilder esqQueryBuilder = new EsqQueryBuilder(Indexes.termQueryList, Indexes.orderedIntersectMap, builderMethod);

        when:
        BooleanQuery.Builder[] bqbL = esqQueryBuilder.getSingleWordQueries(rootGenome3, k)
        Query q0 = bqbL[0].build()
        Query q1 = bqbL[1].build()
        Query q2 = bqbL[2].build()

        then:
        Indexes.termQueryList[0].getTerm().text() == 'god'
        Indexes.termQueryList[1].getTerm().text() == 'space'
        Indexes.termQueryList[2].getTerm().text() == 'jesus'
        Indexes.termQueryList[3].getTerm().text() == 'hockey'
        Indexes.termQueryList[4].getTerm().text() == 'nasa'
        Indexes.termQueryList[5].getTerm().text() == 'game'
        Indexes.termQueryList[6].getTerm().text() == 'team'

        q0.toString(Indexes.FIELD_CONTENTS) == 'god'
        q1.toString(Indexes.FIELD_CONTENTS) == 'space'
        q2.toString(Indexes.FIELD_CONTENTS) == 'jesus'

        when:
        rootGenome3 = new int[]{0, 1, 1}

        bqbL = esqQueryBuilder.getSingleWordQueries(rootGenome3, k)
        q0 = bqbL[0].build()
        q1 = bqbL[1].build()
        q2 = bqbL[2].build()

        then:
        q0.toString(Indexes.FIELD_CONTENTS) == 'god'
        q1.toString(Indexes.FIELD_CONTENTS) == 'space'
        q2.toString(Indexes.FIELD_CONTENTS) == 'space'

        when:
        rootGenome3 = new int[]{0, 1, 3}
        int[] intersectGenome2 = new int[]{0, 1, 0, 1, 0, 1}

        bqbL = esqQueryBuilder.getIntersectQueries(rootGenome3, intersectGenome2, k)
        q0 = bqbL[0].build()
        q1 = bqbL[1].build()
        q2 = bqbL[2].build()

        then:
        q0.toString(Indexes.FIELD_CONTENTS) == 'god faith truth'
        q1.toString(Indexes.FIELD_CONTENTS) == 'space shuttle launch'
        q2.toString(Indexes.FIELD_CONTENTS) == 'hockey league teams'

        when:
        rootGenome3 = new int[]{0, 1, 3}
        intersectGenome2 = new int[]{0, 1, 0, 1, 0, 0}

        bqbL = esqQueryBuilder.getIntersectQueries(rootGenome3, intersectGenome2, k)

        q2 = bqbL[2].build()

        then:
        q2.toString(Indexes.FIELD_CONTENTS) == 'hockey league'
    }
}