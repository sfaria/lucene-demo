package com.demo.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
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

    private static final ExecutorService EX = Executors.newSingleThreadExecutor();

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
        writerConfig.setCommitOnClose(true);
    }

    // -------------------- Default Methods --------------------

    final IndexWriter createWriter() throws IOException {
        return new IndexWriter(indexDir, writerConfig);
    }

    // -------------------- Public Methods --------------------

    public final void performFullIndexing() throws IOException, InterruptedException {
        System.err.println("Performing full indexing...");
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        try (IndexWriter writer = createWriter()) {
            Files.find(rawDataPath, Integer.MAX_VALUE, (Path path, BasicFileAttributes attrs) -> {
                return !attrs.isDirectory() && path.getFileName().toString().endsWith("txt");
            }).forEach(book -> {
                System.err.println("Indexing '" + book.getFileName() + "'...");
                try (InputStream in = new FileInputStream(book.toFile())) {
                    indexRecord(in, writer);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to open stream for indexing.", e);
                }
            });
            writer.commit();
        }
        System.err.println("Indexing finished.");
    }

    public final void addToIndex(InputStream in) throws IOException {
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        EX.submit(() -> {
            try (IndexWriter writer = createWriter()) {
                indexRecord(in, writer);
                writer.commit();
            } catch (IOException e) {
                throw new RuntimeException("Failed to open stream for indexing.", e);
            }
        });
    }

    // -------------------- Private Methods --------------------

    private void indexRecord(InputStream in, IndexWriter writer) throws IOException {
        Document doc = new Document();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            doc.add(new LongField("created", new Date().getTime(), Field.Store.YES));
            doc.add(new TermVectorEnabledTextField("contents", digest(reader)));
            writer.addDocument(doc);
        }
    }

    private String digest(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line.toLowerCase()).append(" ");
        }
        return sb.toString();
    }

}
