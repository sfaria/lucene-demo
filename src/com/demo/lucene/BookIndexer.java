package com.demo.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
public final class BookIndexer {

    // -------------------- Private Statics --------------------

    private static final ExecutorService EX = newFixedThreadPool(getRuntime().availableProcessors());

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

        long startTime = System.currentTimeMillis();
        try (IndexWriter writer = createWriter()) {
            AtomicInteger count = new AtomicInteger(0);
            List<Callable<Void>> tasks = Files.find(rawDataPath, Integer.MAX_VALUE, (Path path, BasicFileAttributes attrs) -> {
                return !attrs.isDirectory() && path.getFileName().toString().endsWith("txt");
            }).map(book -> (Callable<Void>) () -> {
                try (InputStream in = new FileInputStream(book.toFile())) {
                    indexRecord(in, writer);
                    synchronized (count) {
                        int processed = count.incrementAndGet();
                        if (processed % 100 == 0) {
                            writer.commit();
                            System.err.println("Committed. " + processed + " so far.");
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to open stream for indexing.", e);
                }
                return null;
            }).collect(Collectors.toList());

            EX.invokeAll(tasks);

            writer.commit();
        }
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.err.println("Indexing finished in " + TimeUnit.MILLISECONDS.toMinutes(totalTime) + " minutes.");
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
            Book book = parseBook(reader);
            doc.add(new LongField("created", new Date().getTime(), Field.Store.YES));
            doc.add(new StringField("author", book.author, Field.Store.YES));
            doc.add(new StringField("title", book.title, Field.Store.YES));
            doc.add(new TermVectorEnabledTextField("contents", book.content));
            writer.addDocument(doc);
        }
    }

    private Book parseBook(BufferedReader reader) throws IOException {
        Book book = new Book();
        boolean hitEndHeader = false;
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                if (!hitEndHeader) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.startsWith("***") && trimmedLine.endsWith("***")) {
                        hitEndHeader = true;
                    } else {
                        if (trimmedLine.startsWith("Author:")) {
                            book.author = parseHeaderAttribute(trimmedLine);
                        } else if (trimmedLine.startsWith("Title:")) {
                            book.title = parseHeaderAttribute(trimmedLine);
                        }
                    }
                } else {
                    sb.append(line.toLowerCase()).append(" ");
                }
            }
        }
        book.content = sb.toString();
        return book;
    }

    private String parseHeaderAttribute(String line) {
        String[] split = line.split(":");
        if (split.length == 2) {
            return split[1].trim();
        }
        return "";
    }

    // -------------------- Inner Classes --------------------

    private static final class Book {
        String author = "";
        String title = "";
        String content = "";
    }

}
