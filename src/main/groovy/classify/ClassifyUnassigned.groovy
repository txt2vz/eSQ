package classify

import cluster.Effectiveness
import index.IndexEnum
import index.Indexes
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.classification.BM25NBClassifier
import org.apache.lucene.classification.Classifier
import org.apache.lucene.classification.KNearestNeighborClassifier
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.search.similarities.BM25Similarity

enum LuceneClassifyMethod {
    KNN,
    NB
}

class ClassifyUnassigned {

    static Classifier getClassifierForUnassignedDocuments(IndexEnum trainIndex, LuceneClassifyMethod luceneClassifyMethod) {

        Indexes.setIndex(trainIndex)

        TermQuery assignedTQ = new TermQuery(new Term(Indexes.FIELD_ASSIGNED_CLASS, 'unassigned'))
        BooleanQuery.Builder bqb = new BooleanQuery.Builder()
        bqb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);
        bqb.add(assignedTQ, BooleanClause.Occur.MUST_NOT)
        Query unassignedQ = bqb.build()

        TopDocs unAssignedTopDocs = Indexes.indexSearcher.search(unassignedQ, Indexes.indexReader.numDocs())
        ScoreDoc[] unAssignedHits = unAssignedTopDocs.scoreDocs;

        println "unAssignedHits size " + unAssignedHits.size()

        Classifier classifier

        switch (luceneClassifyMethod) {

            case LuceneClassifyMethod.KNN:
                classifier = new KNearestNeighborClassifier(
                        Indexes.indexReader,
                        new BM25Similarity(),
                        new StandardAnalyzer(),
                        unassignedQ,
                        20,
                        3,
                        1,
                        Indexes.FIELD_ASSIGNED_CLASS,
                        Indexes.FIELD_CONTENTS
                )
                break;

            case LuceneClassifyMethod.NB:
                classifier = new BM25NBClassifier(
                        Indexes.indexReader,
                        new StandardAnalyzer(),
                        unassignedQ,
                        Indexes.FIELD_ASSIGNED_CLASS,
                        Indexes.FIELD_CONTENTS
                )
                break;
        }

        return classifier
    }

    static void main(String[] args) {
        Classifier classifier = getClassifierForUnassignedDocuments(IndexEnum.CRISIS3, LuceneClassifyMethod.KNN)
        Effectiveness.classifierEffectiveness(classifier, IndexEnum.CRISIS3TEST, IndexEnum.CRISIS3TEST.numberOfCategories)
    }
}
