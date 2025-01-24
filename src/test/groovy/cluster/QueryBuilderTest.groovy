package cluster

import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import spock.lang.Specification

class QueryBuilderTest extends Specification {

    def "queryBuildTest" (){
        when:
        List<TermQuery> tql = [
        new TermQuery(new Term(Indexes.FIELD_CONTENTS, "god") ),
        new TermQuery(new Term(Indexes.FIELD_CONTENTS, "hockey") ),
        new TermQuery(new Term(Indexes.FIELD_CONTENTS, "nasa") ),
        new TermQuery(new Term(Indexes.FIELD_CONTENTS, "jesus") ),
        new TermQuery(new Term(Indexes.FIELD_CONTENTS, "space") ),
        new TermQuery(new Term(Indexes.FIELD_CONTENTS, "team") )
         ]


        then:
        tql[0].getTerm().text() == 'god'

    }
}
