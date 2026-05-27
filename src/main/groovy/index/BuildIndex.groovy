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

    final static  int MAX_DOCS_PER_DIRECTORY = 10000

    BuildIndex(IndexEnum index) {

        String indexPath = index.indexPath
        String docsPath =  index.docsPath
   
        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = Indexes.analyzer
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)
        iwc.setSimilarity {(Indexes.similarity)}

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

                    String parent = file.getParent()                

                    String catName = parent.substring(parent.lastIndexOf(File.separator) + 1, parent.length())
                    Field catNameField = new StringField(Indexes.FIELD_CATEGORY_NAME, catName.replaceAll(/\W/, '').toLowerCase(), Field.Store.YES)
                    doc.add(catNameField)

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
        println "Total docs: ${indexReader.numDocs()}"

        final Date end = new Date()
        TimeDuration duration = TimeCategory.minus(end, start)
        println "Duration: $duration"

        println "catsNameFreq: $catsNameFreq"

        IndexUtils.categoryFrequencies(indexReader, true)

        println "numDocs " + indexReader.numDocs()
        println "End ***************************************************************"
    }

    static main(args) {
       //new BuildIndex(IndexEnum.CRISIS6)
   
        for (index in IndexEnum.values()) {
            new BuildIndex(index)
           
            println "$index numDocs: ${index.getIndexReader().numDocs()}"            
        }
    }
}