package index

import groovy.time.TimeCategory
import groovy.time.TimeDuration
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
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

class BuildIndex {

    final static  int MAX_DOCS_PER_DIRECTORY = 5000

    BuildIndex() {

        String indexName = 'NG3'
        String indexPath = 'indexes' + /\$indexName/
        String docsPath =  'datasets' + /\$indexName/

        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = Indexes.analyzer
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)

//store doc counts for each category
        def catsNameFreq = [:]

// Create a new index in the directory, removing any previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE)
        IndexWriter writer = new IndexWriter(directory, iwc)
        Date start = new Date()
        println("Indexing to directory: $indexPath  from: $docsPath ...")

        def categoryNumber = 0

        int totalDocCount = 0
        new File(docsPath).eachDir {

            int dirCount = 0
            it.eachFileRecurse { file ->

                if (!file.hidden && file.exists() && file.canRead() && !file.isDirectory() && dirCount < MAX_DOCS_PER_DIRECTORY) {
                    Document doc = new Document()

                    Field catNumberField = new StringField(Indexes.FIELD_CATEGORY_NUMBER, String.valueOf(categoryNumber), Field.Store.YES)
                    doc.add(catNumberField)

                    //non-alpha characters cause a problem when identifying a document for delete.
                    String fileName = file.getName().replaceAll(/\W/, '').toLowerCase() + 'id' + totalDocCount
                    Field documentIDfield = new StringField(Indexes.FIELD_DOCUMENT_ID, fileName, Field.Store.YES)
                    doc.add(documentIDfield)

                    String parent = file.getParent()
                   // String grandParent = file.getParentFile().getParent()

                    String catName = parent.substring(parent.lastIndexOf(File.separator) + 1, parent.length())
                    Field catNameField = new StringField(Indexes.FIELD_CATEGORY_NAME, catName.replaceAll(/\W/, '').toLowerCase(), Field.Store.YES)
                    doc.add(catNameField)

                    Field assignedClass = new StringField(Indexes.FIELD_QUERY_ASSIGNED_CLUSTER, 'unassigned', Field.Store.YES)
                    doc.add(assignedClass)

                    doc.add(new TextField(Indexes.FIELD_CONTENTS, file.text, Field.Store.YES))

                    def n = catsNameFreq.get((catName)) ?: 0
                    catsNameFreq.put((catName), n + 1)

                    writer.addDocument(doc)
                    dirCount++
                    totalDocCount++
                }
            }
            categoryNumber++
        }

        writer.commit()
        writer.close()
        IndexReader indexReader = DirectoryReader.open(directory)
        IndexSearcher indexSearcher = new IndexSearcher(indexReader)
        println "Total docs: ${indexReader.numDocs()}"

        final Date end = new Date()
        TimeDuration duration = TimeCategory.minus(end, start)
        println "Duration: $duration"

        println "catsNameFreq: $catsNameFreq"

        IndexUtils.categoryFrequencies(indexSearcher, true)

        println "numDocs " + indexReader.numDocs()
        println "End ***************************************************************"
    }

    static main(args) {
        new BuildIndex()
    }
}