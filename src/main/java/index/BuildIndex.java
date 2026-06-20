package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class BuildIndex {

    public static final int MAX_DOCS_PER_DIRECTORY = 10000;

    public BuildIndex(IndexEnum index) throws IOException {
        String indexPath = index.getIndexPath();
        String docsPath = index.getDocsPath();

        Path path = Paths.get(indexPath);
        Directory directory = FSDirectory.open(path);
        Analyzer analyzer = Indexes.analyzer;
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        // store doc counts for each category
        Map<String, Integer> catsNameFreq = new HashMap<>();

        iwc.setOpenMode(OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(directory, iwc);
        Instant start = Instant.now();
        System.out.println("Indexing to directory: " + indexPath + " from: " + docsPath + " ...");

        File docsDir = new File(docsPath);
        int categoryNumber = 0;
        int totalDocCount = 0;

        if (docsDir.exists() && docsDir.isDirectory()) {
            File[] dirs = docsDir.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    int dirCount = 0;
                    File[] files = dir.listFiles();
                    if (files == null) {
                        continue;
                    }
                    for (File file : files) {
                        if (!file.isHidden() && file.exists() && file.canRead() && file.isFile() && dirCount < MAX_DOCS_PER_DIRECTORY) {
                            Document doc = new Document();
                            String catName = dir.getName();
                            Field catNameField = new StringField(Indexes.FIELD_CATEGORY_NAME, catName.replaceAll("\\W", "").toLowerCase(), Field.Store.YES);
                            doc.add(catNameField);
                            doc.add(new TextField(Indexes.FIELD_CONTENTS, java.nio.file.Files.readString(file.toPath()), Field.Store.YES));

                            catsNameFreq.put(catName, catsNameFreq.getOrDefault(catName, 0) + 1);
                            writer.addDocument(doc);
                            dirCount++;
                            totalDocCount++;
                        }
                    }
                    categoryNumber++;
                }
            }
        }

        writer.commit();
        writer.close();
        IndexReader indexReader = DirectoryReader.open(directory);
        System.out.println("Total docs: " + indexReader.numDocs());

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println("Duration: " + duration);
        System.out.println("catsNameFreq: " + catsNameFreq);

        IndexUtils.categoryFrequencies(indexReader, true);
        System.out.println("numDocs " + indexReader.numDocs());
        System.out.println("End ***************************************************************");
    }

    public static void main(String[] args) throws IOException {
        for (IndexEnum index : IndexEnum.values()) {
            new BuildIndex(index);
            System.out.println(index + " numDocs: " + index.getIndexReader().numDocs());
        }
    }
}
