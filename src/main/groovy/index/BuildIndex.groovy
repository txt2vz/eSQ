package index

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader

import java.nio.file.Path
import java.nio.file.Paths

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

class BuildIndex {

    static main(args) {
        new BuildIndex()
    }

    BuildIndex() {

        String indexPath =
          //      'indexes/R4'
       //'indexes/NG4'
                'indexes/NG4N'

        String docsPath =
        //     /C:\Data\NG4/
        /C:\Data\NG4N/
         //         /C:\Data\R4/


        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = Indexes.analyzer
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)

//store doc counts for each category
        def catsNameFreq = [:]

// Create a new index in the directory, removing any
// previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE)
        IndexWriter writer = new IndexWriter(directory, iwc)
        Date start = new Date();
        println("Indexing to directory: $indexPath  from: $docsPath ...")

        def categoryNumber = 0

        int docCount = 0
        new File(docsPath).eachDir {

            int dirCount = 0
            it.eachFileRecurse { file ->

                if (!file.hidden && file.exists() && file.canRead() && !file.isDirectory() && dirCount < 600) {
                    Document doc = new Document()

                    Field catNumberField = new StringField(Indexes.FIELD_CATEGORY_NUMBER, String.valueOf(categoryNumber), Field.Store.YES);
                    doc.add(catNumberField)

                    //non-alpha characters cause a problem when identifying a document for delete.
                    String fileName = file.getName().replaceAll(/\W/, '').toLowerCase() + 'id' + docCount
                    Field documentIDfield = new StringField(Indexes.FIELD_DOCUMENT_ID, fileName, Field.Store.YES)
                    doc.add(documentIDfield)


                    String parent = file.getParent()
                   // String grandParent = file.getParentFile().getParent()

                    String catName = parent.substring(parent.lastIndexOf(File.separator) + 1, parent.length())
                    Field catNameField = new StringField(Indexes.FIELD_CATEGORY_NAME, catName.replaceAll(/\W/, '').toLowerCase(), Field.Store.YES);
                    doc.add(catNameField)

                    Field assignedClass = new StringField(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER, 'unassigned', Field.Store.YES);
                    doc.add(assignedClass)

                    doc.add(new TextField(Indexes.FIELD_CONTENTS, file.text, Field.Store.YES))

                    def n = catsNameFreq.get((catName)) ?: 0
                    catsNameFreq.put((catName), n + 1)

                    writer.addDocument(doc)
                    dirCount++
                    docCount++
                }
            }
            categoryNumber++
        }
        println "Total docs: " + writer.maxDoc()
        writer.commit()
        writer.close()
        IndexReader indexReader = DirectoryReader.open(directory)
        IndexSearcher indexSearcher = new IndexSearcher(indexReader)
        TotalHitCountCollector trainCollector = new TotalHitCountCollector();
        final TermQuery trainQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, "train"))

        TotalHitCountCollector testCollector = new TotalHitCountCollector();
        final TermQuery testQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, "test"))

        indexSearcher.search(trainQ, trainCollector);
        def trainTotal = trainCollector.getTotalHits();

        indexSearcher.search(testQ, testCollector);
        def testTotal = testCollector.getTotalHits();

        Date end = new Date();
        println(end.getTime() - start.getTime() + " total milliseconds");
        println "testTotal $testTotal trainTotal $trainTotal"
        println "catsNameFreq $catsNameFreq"

         Indexes.setIndex(IndexEnum.CRISIS3b)
         IndexUtils.categoryFrequencies(Indexes.indexSearcher)  //.showCategoryFrequencies(Indexes.indexSearcher)

        println "numDocs " + indexReader.numDocs()
        println "End ***************************************************************"
    }
}