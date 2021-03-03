package index

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

class BuildNGIndex {

    static main(args) {
        new BuildNGIndex()
    }

    BuildNGIndex() {


        final String docsPath = // /C:\Users\aceslh\Dataset\GAclusterPaper2018\r4P/
        // /C:\Users\aceslh\Dataset\GAclusterPaper2018\20NG6GraphicsHockeyCryptSpaceChristianGuns/
        /C:\Users\aceslh\Dataset\r5/


        final String indexPath = //'indexes/classic4_500'
                //'indexes/20NG'

        //        'indexes/20NG3SpaceHockeyChristian'
            //    'indexes/20NG6GraphicsHockeyCryptSpaceChristianGuns'
                'indexes/R5'


        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = //new EnglishAnalyzer();  //with stemming
                new StandardAnalyzer()
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)

        def catsNameFreq = [:]
        def catsNumberFreq = [:]

// Create a new index in the directory, removing any
// previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE)
        IndexWriter writer = new IndexWriter(directory, iwc)
        //  IndexSearcher indexSearcher = new IndexSearcher(writer.getReader())
        Date start = new Date();
        println("Indexing to directory: $indexPath  from: $docsPath ...")

        def categoryNumber = 0

        new File(docsPath).eachDir {
            categoryNumber++
            int x = 0
            it.eachFileRecurse { file ->
                x++

                if (!file.hidden && file.exists() && file.canRead() && !file.isDirectory() )//&& x <= 100) // && categoryNumber <3)

                {
                    def doc = new Document()

                    Field catNumberField = new StringField(Indexes.FIELD_CATEGORY_NUMBER, String.valueOf(categoryNumber), Field.Store.YES);
                    doc.add(catNumberField)

                    Field pathField = new StringField(Indexes.FIELD_PATH, file.getPath(), Field.Store.YES);
                    doc.add(pathField);

                    String parent = file.getParent()
                    String grandParent = file.getParentFile().getParent()

                    def catName


                        catName = parent.substring(parent.lastIndexOf(File.separator) + 1, parent.length())

                    Field catNameField = new StringField(Indexes.FIELD_CATEGORY_NAME, catName, Field.Store.YES);
                    doc.add(catNameField)

                  //  String test_train
               //     if (file.canonicalPath.contains("test")) test_train = "test" else test_train = "train"
                    //   println "cannonicla ptath is" + file.canonicalPath
                    //    println "test train $test_train"
                    //   println ""
                  //  Field ttField = new StringField(Indexes.FIELD_TEST_TRAIN, test_train, Field.Store.YES)
                //    doc.add(ttField)

                    doc.add(new TextField(Indexes.FIELD_CONTENTS, file.text, Field.Store.YES))

                    def n = catsNameFreq.get((catName)) ?: 0
                    catsNameFreq.put((catName), n + 1)

                    int y = catsNumberFreq.get((categoryNumber.toString())) ?: 0
                    catsNumberFreq.put((String.valueOf(categoryNumber)), y + 1)

                    writer.addDocument(doc)
                }
            }
        }
        println "Total docs: " + writer.maxDoc()
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
        println "catsNumberFreq $catsNumberFreq"

        println "End ***************************************************************"
    }
}