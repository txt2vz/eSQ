package classify

import groovy.transform.CompileStatic
import index.IndexUtils
import index.Indexes
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.classification.Classifier
import org.apache.lucene.classification.KNearestFuzzyClassifier
import org.apache.lucene.classification.KNearestNeighborClassifier
import org.apache.lucene.classification.BM25NBClassifier

import org.apache.lucene.classification.utils.ConfusionMatrixGenerator
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.StoredFields
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

enum LuceneClassifyMethod {
    KNN,
    FuzzyKNN,
    BM25NBClassifier
}

@CompileStatic
class EsqClassify {

    private Query[] queriesReturningDistinctDocuments
    private Query[] queriesOriginal
    private boolean checkConfusionMatrix = false
    IndexReader reader = Indexes.indexSearcher.getIndexReader();
    StoredFields storedFields = reader.storedFields();
    Set<String> qSet = [] as Set

    EsqClassify(Query[] queries, Query[] modifiedQueries) {

        queriesOriginal = queries
        queriesReturningDistinctDocuments = modifiedQueries
        assert queriesOriginal.size() == queriesReturningDistinctDocuments.size()
    }

    void updateAssignedClusterField(boolean useQueriesReturningDistinctDocuments) {

        IndexWriter indexWriter = setAllUnassigned()
        Query[] queries = useQueriesReturningDistinctDocuments ? queriesReturningDistinctDocuments : queriesOriginal

        int counter = 0
        String q
        for (int i = 0; i < queries.size(); i++) {

            //queryOriginal is an easier to read cluster label
            q = queriesOriginal[i].toString(Indexes.FIELD_CONTENTS)
            qSet << q

            TopDocs topDocs = Indexes.indexSearcher.search(queries[i], Integer.MAX_VALUE)
            ScoreDoc[] hits = topDocs.scoreDocs

            for (ScoreDoc sd : hits) {

                Document d = storedFields.document(sd.doc);
                d.removeField(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER)

                Field assignedClass = new StringField(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER, q, Field.Store.YES)
                d.add(assignedClass)

                Term t = new Term(Indexes.FIELD_DOCUMENT_ID, d.get(Indexes.FIELD_DOCUMENT_ID))
                indexWriter.updateDocument(t, d)

                counter++
            }
        }
        println("qSet $qSet")
        println "$counter docs updated in UpdateAssignedFieldIndex"

        indexWriter.forceMerge(1)
        indexWriter.commit()

        indexWriter.close()
        Indexes.setIndex(Indexes.index)
        IndexUtils.categoryFrequencies(Indexes.indexReader, false)
    }

    Classifier getClassifier(LuceneClassifyMethod luceneClassifyMethod, final int k_for_knn = 7) {
        TermQuery assignedTQ = new TermQuery(new Term(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER, 'unassigned'))
        BooleanQuery.Builder bqb = new BooleanQuery.Builder()
        bqb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD)
        bqb.add(assignedTQ, BooleanClause.Occur.MUST_NOT)
        Query queryToAssignDocumentToCluster = bqb.build()

        TopDocs unAssignedTopDocs = Indexes.indexSearcher.search(queryToAssignDocumentToCluster, Indexes.indexReader.numDocs())
        ScoreDoc[] unAssignedHits = unAssignedTopDocs.scoreDocs

        println "In EsqClassify unAssignedHits size: " + unAssignedHits.size()

        Classifier classifier =

                switch (luceneClassifyMethod) {
                    case LuceneClassifyMethod.FuzzyKNN ->
                        new KNearestFuzzyClassifier(
                                Indexes.indexReader,
                                new BM25Similarity(),
                                Indexes.analyzer,
                                queryToAssignDocumentToCluster,
                                k_for_knn,
                                Indexes.FIELD_QUERY_ASSIGNED_CLUSTER,
                                Indexes.FIELD_CONTENTS
                        )

                    case LuceneClassifyMethod.KNN ->
                        new KNearestNeighborClassifier(
                                Indexes.indexReader,
                                new BM25Similarity(),
                                Indexes.analyzer,
                                queryToAssignDocumentToCluster,
                                k_for_knn,
                                3,
                                1,
                                Indexes.FIELD_QUERY_ASSIGNED_CLUSTER,
                                Indexes.FIELD_CONTENTS
                        )

                    case LuceneClassifyMethod.BM25NBClassifier ->
                        new BM25NBClassifier(
                                Indexes.indexReader,
                                Indexes.analyzer,
                                queryToAssignDocumentToCluster,
                                Indexes.FIELD_QUERY_ASSIGNED_CLUSTER,
                                Indexes.FIELD_CONTENTS
                        )
                }

        return classifier
    }

    //don't think you can use this as labels change each time and would need test train split
    public double getConfusionMatrixF1(Classifier classifier) {
        ConfusionMatrixGenerator.ConfusionMatrix confusionMatrix =
                ConfusionMatrixGenerator.getConfusionMatrix(
                        Indexes.indexReader,
                        classifier,
                        Indexes.FIELD_QUERY_ASSIGNED_CLUSTER,  //the ground truth category
                        Indexes.FIELD_CONTENTS,   //the field to analyse
                        80000       // Timeout in milliseconds
                );
        println("Classifier: $classifier")
        String q = qSet[0]
        println("Confusion matrix F1 for query $q:  ${confusionMatrix.getF1Measure(q)}  overall confusion f1: ${confusionMatrix.getF1Measure()}")
        return confusionMatrix.getF1Measure()
    }

    private IndexWriter setAllUnassigned() {
        IndexWriter indexWriter = prepareIndex()

        TopDocs topDocsAll = Indexes.indexSearcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE)
        ScoreDoc[] hitsAll = topDocsAll.scoreDocs

        int counter = 0
        for (ScoreDoc sd : hitsAll) {

            Document d = storedFields.document(sd.doc);
            //Document d = Indexes.indexSearcher.doc(sd.doc)
            d.removeField(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER)
            Field assignedClass = new StringField(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER, 'unassigned', Field.Store.YES)
            d.add(assignedClass)

            Term t = new Term(Indexes.FIELD_DOCUMENT_ID, d.get(Indexes.FIELD_DOCUMENT_ID))
            indexWriter.updateDocument(t, d)
            counter++
        }

        println "In EsqClassify setAllUnassigned $counter updated"
        indexWriter.forceMerge(1)
        indexWriter.commit()
        return indexWriter
    }

    private IndexWriter prepareIndex() {
        //  Indexes.setIndex(Indexes.index)
        String indexPath = Indexes.index.pathString
        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = Indexes.analyzer//new StandardAnalyzer()
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)
        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND)
        return new IndexWriter(directory, iwc)
    }
}
