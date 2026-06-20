package index;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public enum IndexEnum {

    CRISIS3("indexes/Crisis3", "datasets/Crisis3", 3),
    CRISIS4("indexes/crisis4", "datasets/crisis4", 4),
    CRISIS6("indexes/CrisisLexT6", "datasets/CrisisLexT6", 6),
    NG3("indexes/NG3", "datasets/NG3", 3),
    NG5("indexes/NG5", "datasets/NG5", 5),
    NG6("indexes/NG6", "datasets/NG6", 6),
    R4("indexes/R4", "datasets/R4", 4),
    R5("indexes/R5", "datasets/R5", 5),
    R6("indexes/R6", "datasets/R6", 6);

    private final String indexPath;
    private final String docsPath;
    private final int numberOfClasses;

    IndexEnum(String indexPath, String docsPath, int numberOfClasses) {
        this.indexPath = indexPath;
        this.docsPath = docsPath;
        this.numberOfClasses = numberOfClasses;
    }

    public String getIndexPath() {
        return indexPath;
    }

    public String getDocsPath() {
        return docsPath;
    }

    public int getNumberOfClasses() {
        return numberOfClasses;
    }

    @Override
    public String toString() {
        return name() + " indexPath: " + indexPath + " docsPath: " + docsPath + " numberOfClasses: " + numberOfClasses;
    }

    public IndexReader getIndexReader() throws IOException {
        Path path = Paths.get(indexPath);
        Directory directory = FSDirectory.open(path);
        return DirectoryReader.open(directory);
    }

    public IndexSearcher getIndexSearcher() throws IOException {
        Path path = Paths.get(indexPath);
        Directory directory = FSDirectory.open(path);
        IndexReader ir = DirectoryReader.open(directory);
        return new IndexSearcher(ir);
    }
}
