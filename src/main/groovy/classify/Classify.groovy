package classify

import groovy.transform.CompileStatic
import index.IndexEnum
import index.IndexUtils
import index.Indexes
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.classification.BM25NBClassifier
import org.apache.lucene.classification.Classifier
import org.apache.lucene.classification.KNearestNeighborClassifier
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

enum LuceneClassifyMethod {
    KNN,
    NB
}

@CompileStatic
class Classify {

    private IndexEnum indexEnum
    private Set<Query> querySet

    Classify(IndexEnum ie, Set<Query> queries) {
        indexEnum = ie
        querySet = queries
    }

    void updateAssignedField() {

        IndexWriter indexWriter = setAllUnassigned()

        int counter = 0
        querySet.each { Query query ->

            TopDocs topDocs = Indexes.indexSearcher.search(query, Integer.MAX_VALUE)
            ScoreDoc[] hits = topDocs.scoreDocs

            println("$counter query ${query.toString(Indexes.FIELD_CONTENTS)}")

            for (ScoreDoc sd : hits) {

                Document d = Indexes.indexSearcher.doc(sd.doc)

                d.removeField(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER)
                Field assignedClass = new StringField(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER, query.toString(Indexes.FIELD_CONTENTS), Field.Store.YES);
                d.add(assignedClass)

                Term t = new Term(Indexes.FIELD_DOCUMENT_ID, d.get(Indexes.FIELD_DOCUMENT_ID))
                indexWriter.updateDocument(t, d)

                counter++
            }
        }

        println "$counter docs updated in UpdateAssignedFieldIndex"

        indexWriter.forceMerge(1)
        indexWriter.commit()
        println "Max docs: " + indexWriter.maxDoc() + " numDocs: " + indexWriter.numDocs()

        indexWriter.close()

        Indexes.setIndex(indexEnum)
        IndexUtils.categoryFrequencies(Indexes.indexSearcher)
    }

    //modify queries so that they do not return documents returned by any other query
    void modifyQuerySoDocsReturnedByOnlyOneQuery() {

        Set<Query> docInOneClusterQueries = []
        for (int i = 0; i < querySet.size(); i++) {
            Query q = querySet[i]

            BooleanQuery.Builder bqbOneCategoryOnly = new BooleanQuery.Builder()
            bqbOneCategoryOnly.add(q, BooleanClause.Occur.SHOULD)

            for (int j = 0; j < querySet.size(); j++) {
                if (j != i) {
                    bqbOneCategoryOnly.add(querySet[j], BooleanClause.Occur.MUST_NOT)
                }
            }
            docInOneClusterQueries << bqbOneCategoryOnly.build()
        }
        println "uniqueries $docInOneClusterQueries"
        querySet = docInOneClusterQueries
    }

    Classifier getClassifier(LuceneClassifyMethod luceneClassifyMethod) {
        TermQuery assignedTQ = new TermQuery(new Term(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER,  'unassigned'))
        BooleanQuery.Builder bqb = new BooleanQuery.Builder()
        bqb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);
        bqb.add(assignedTQ, BooleanClause.Occur.MUST_NOT)
        Query unassignedQ = bqb.build()

        TopDocs unAssignedTopDocs = Indexes.indexSearcher.search(unassignedQ, Indexes.indexReader.numDocs())
        ScoreDoc[] unAssignedHits = unAssignedTopDocs.scoreDocs;

        println " in classifyUnassigned unAssignedHits size " + unAssignedHits.size()

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
                        Indexes.FIELD_QUERY_ASSIGNED_CLUSTER,
                        Indexes.FIELD_CONTENTS
                )
                break;

            case LuceneClassifyMethod.NB:
                classifier = new BM25NBClassifier(
                        Indexes.indexReader,
                        new StandardAnalyzer(),
                        unassignedQ,
                        Indexes.FIELD_QUERY_ASSIGNED_CLUSTER,
                        Indexes.FIELD_CONTENTS
                )
                break;
        }

        return classifier
    }


    private IndexWriter setAllUnassigned() {
        IndexWriter indexWriter = prepareIndex()

        TopDocs topDocsAll = Indexes.indexSearcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE)
        ScoreDoc[] hitsAll = topDocsAll.scoreDocs

        int counter = 0
        for (ScoreDoc sd : hitsAll) {

            Document d = Indexes.indexSearcher.doc(sd.doc)
            d.removeField(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER)
            Field assignedClass = new StringField(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER, 'unassigned', Field.Store.YES);
            d.add(assignedClass)

            Term t = new Term(Indexes.FIELD_DOCUMENT_ID, d.get(Indexes.FIELD_DOCUMENT_ID))
            indexWriter.updateDocument(t, d)
            counter++
        }

        println " in setAllUnassigned $counter updated"
        indexWriter.forceMerge(1)
        indexWriter.commit()
       return indexWriter
    }

    private IndexWriter prepareIndex() {
        Indexes.setIndex(indexEnum)
        String indexPath = Indexes.index.pathString
        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = new StandardAnalyzer()
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)
        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND)
        IndexWriter indexWriter = new IndexWriter(directory, iwc)
        indexWriter
    }
}
