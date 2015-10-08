package com.demo.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
public final class BookIndexer {

    // -------------------- Private Statics --------------------

    private static final ExecutorService EX = Executors.newWorkStealingPool(4);

    // -------------------- Private Variables --------------------

    private final FSDirectory indexDir;
    private final IndexWriterConfig writerConfig;
    private final Path rawDataPath;

    // -------------------- Constructors --------------------

    public BookIndexer(Path rawDataPath, Path indexPath) throws IOException {
        this.rawDataPath = rawDataPath;
        this.indexDir = FSDirectory.open(indexPath);
        this.writerConfig = new IndexWriterConfig(new StandardAnalyzer());
        writerConfig.setRAMBufferSizeMB(512d);
    }

    // -------------------- Public Methods --------------------

    public final void performFullIndexing() throws IOException {
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (IndexWriter writer = new IndexWriter(indexDir, writerConfig)) {
            Files.find(rawDataPath, Integer.MAX_VALUE, (Path path, BasicFileAttributes attrs) -> {
                return !attrs.isDirectory() && path.getFileName().endsWith("txt");
            }).forEach(book -> EX.submit(() -> {
               try (InputStream in = new FileInputStream(book.toFile())) {
                   indexRecord(in, writer);
               } catch (IOException e) {
                   throw new RuntimeException("Failed to open stream for indexing.", e);
               }
            }));
        }
    }

    public final void addToIndex(InputStream in) throws IOException {
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        try (IndexWriter writer = new IndexWriter(indexDir, writerConfig)) {
            EX.submit(() -> indexRecord(in, writer));
        }
    }

    // -------------------- Private Methods --------------------

    private void indexRecord(InputStream in, IndexWriter writer) {
        Document doc = new Document();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            doc.add(new LongField("created", new Date().getTime(), Field.Store.YES));
            doc.add(new TextField("contents", digest(reader), Field.Store.YES));
            writer.addDocument(doc);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open stream for indexing.", e);
        }
    }

    private String digest(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
