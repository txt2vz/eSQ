package index

import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.TermQuery
import spock.lang.Specification

class IndexUtilsTest extends Specification {

    def 'GetMostFrequentCategoryForQuery'() {
        setup:
        Indexes.setIndex(IndexEnum.NG3)

        TermQuery spaceQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'space'))
        TermQuery orbitQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'orbit'))

        TermQuery hockeyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'hockey'))
        TermQuery gameQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'game'))

        TermQuery emptyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, '?jkXX'))

        when:
        BooleanQuery.Builder spaceBQB = new BooleanQuery.Builder().add(spaceQuery, BooleanClause.Occur.SHOULD)
        spaceBQB.add(orbitQuery, BooleanClause.Occur.SHOULD)
        BooleanQuery.Builder hockeyBQB = new BooleanQuery.Builder().add(hockeyQuery, BooleanClause.Occur.SHOULD)
        hockeyBQB.add(gameQuery, BooleanClause.Occur.SHOULD)

        then:
        'scispace' == IndexUtils.getMostFrequentCategoryForQuery(spaceBQB.build()).v1
        'recsporthockey'  == IndexUtils.getMostFrequentCategoryForQuery(hockeyBQB.build()).v1

    }

    def 'GetMostFrequentCategoryForQuery R4 '() {

        setup:
        Indexes.setIndex(IndexEnum.R4)
        TermQuery catQ

        when:
        catQ = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME,'grain'))
        Tuple3 t3 = IndexUtils.getMostFrequentCategoryForQuery(catQ)

        then:
        t3.v1 == 'grain'
        t3.v2 ==  200
        t3.v3 ==  200

//        when:
//        catQ = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'bpd'));  //barrels per day
//        t3 = IndexUtils.getMostFrequentCategoryForQuery(catQ)
//
//        then:
//        t3.v1 == 'crude'
//        t3.v2 == 15
//        t3.v3 == 15
//
//        when:
//        catQ = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'oil'))
//        t3 = IndexUtils.getMostFrequentCategoryForQuery(catQ)
//
//        then:
//        t3.v1  == 'crude'
//        t3.v2 == 62
//        t3.v3 == 75

    }
}
