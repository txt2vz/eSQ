package index

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

//import org.apache.lucene.queryparser.classic.QueryParser
class IndexCrisisFromCSV {

    // Create Lucene index in this directory
    Path indexPath = Paths.get('indexes/crisis3FireBombFloodTest')
    Path docsPath = Paths.get(/C:\Users\aceslh\OneDrive - Sheffield Hallam University\DataSets\crisisData3/)
    Directory directory = FSDirectory.open(indexPath)
    Analyzer analyzer = //new EnglishAnalyzer();  //with stemming  //new WhitespaceAnalyzer()
                    new StandardAnalyzer();

    def catsFreq = [:]

    static main(args) {
        def i = new IndexCrisisFromCSV()
        i.buildIndex()
    }

    def buildIndex() {
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);

        IndexWriter writer = new IndexWriter(directory, iwc);

        Date start = new Date();
        println("Indexing to directory $indexPath ...");

        //	println "docsPath $docsPath parent" + docsPath.getParent()
        int categoryNumber = 0

        int id = 0


        docsPath.toFile().eachFileRecurse { file ->

            String catName = file.getName().take(14).replaceAll(/\W/, '').toLowerCase()
            catName = catName.replaceAll('_', '')
            println "File: $file  CatName: $catName"
			int tweetCountPerFile = 0
         //   catName.replaceAll(/\W/, '').toLowerCase()
            file.splitEachLine(',') { fields ->

                if (tweetCountPerFile < 500) {

                    def n = catsFreq.get((catName)) ?: 0
                    if (n < 500) {

                        catsFreq.put((catName), n + 1)

                        String tweetID = fields[0]

                        def textBody = fields[1]
                        //def tweetID = fields[0]
                        def doc = new Document()

                        if (textBody != " ") {
                            doc.add(new TextField(Indexes.FIELD_CONTENTS, textBody, Field.Store.YES))
                        }

                        Field catNameField = new StringField(Indexes.FIELD_CATEGORY_NAME, catName.replaceAll(/\W/, '').toLowerCase(), Field.Store.YES);
                      //  Field catNameField = new StringField(Indexes.FIELD_CATEGORY_NAME, catName, Field.Store.YES);
                        doc.add(catNameField)

                        Field catNumberField = new StringField(Indexes.FIELD_CATEGORY_NUMBER, String.valueOf(categoryNumber), Field.Store.YES);
                        doc.add(catNumberField)

                        String idField = 'id' + id

                        Field documentIDfield = new StringField(Indexes.FIELD_DOCUMENT_ID, idField, Field.Store.YES)
                        doc.add(documentIDfield)

                        if (id < 10)  println "idfield $idField"

                        String test_train
                        if (n % 2 == 0) test_train = 'test' else test_train = 'train'
                        Field ttField = new StringField(Indexes.FIELD_TEST_TRAIN, test_train, Field.Store.YES)
                        doc.add(ttField)

                        Field assignedClass = new StringField(Indexes.FIELD_ASSIGNED_CLASS, 'unassigned', Field.Store.YES);
                        doc.add(assignedClass)

                        writer.addDocument(doc)
                        id++

                    }
                }
                tweetCountPerFile++
            }
            categoryNumber++

        }
        println "catsFreq $catsFreq"
        println "Total docs in index: ${writer.maxDoc()}"
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

        println "testTotal $testTotal trainTotal $trainTotal"

        println 'done...'
    }
}