package com.demo.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
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
import java.util.concurrent.TimeUnit;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
public final class BookIndexer {

    // -------------------- Private Statics --------------------

    private static final ExecutorService EX = Executors.newSingleThreadExecutor();

    private static final FieldType TYPE = new FieldType();
    static {
        TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        TYPE.setTokenized(true);
        TYPE.setStored(true);
        TYPE.setStoreTermVectorOffsets(true);
        TYPE.setStoreTermVectorPositions(true);
        TYPE.freeze();
    }

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
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        ExecutorService executor = Executors.newWorkStealingPool(4);
        try (IndexWriter writer = createWriter()) {
            Files.find(rawDataPath, Integer.MAX_VALUE, (Path path, BasicFileAttributes attrs) -> {
                return !attrs.isDirectory() && path.getFileName().endsWith("txt");
            }).forEach(book -> executor.submit(() -> {
               try (InputStream in = new FileInputStream(book.toFile())) {
                   indexRecord(in, writer);
               } catch (IOException e) {
                   throw new RuntimeException("Failed to open stream for indexing.", e);
               }
            }));
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }
    }

    public final void addToIndex(InputStream in) throws IOException {
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        try (IndexWriter writer = createWriter()) {
            EX.submit(() -> indexRecord(in, writer));
        }
    }

    // -------------------- Private Methods --------------------

    private void indexRecord(InputStream in, IndexWriter writer) {
        Document doc = new Document();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            doc.add(new LongField("created", new Date().getTime(), TYPE));
            doc.add(new TextField("contents", digest(reader), Field.Store.YES));
            writer.addDocument(doc);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open stream for indexing.", e);
        }
    }

    private String digest(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
