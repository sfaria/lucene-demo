package com.demo.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
public final class BookIndexer {

    // -------------------- Private Statics --------------------

    private static final ExecutorService EX = newFixedThreadPool(getRuntime().availableProcessors());

    // -------------------- Private Variables --------------------

    private final Path rawDataPath;
    private final Path indexPath;
    private final QueryParser queryParser;

    // -------------------- Constructors --------------------

    public BookIndexer(Path rawDataPath, Path indexPath) throws IOException {
        this.rawDataPath = rawDataPath;
        this.indexPath = indexPath;
        this.queryParser = new QueryParser("id", new StandardAnalyzer());
    }

    // -------------------- Public Methods --------------------

    public final void performFullIndexing() throws IOException, InterruptedException {
        System.err.println("Performing full indexing...");

        AtomicInteger count = new AtomicInteger(0);
        List<Callable<Void>> tasks = new ArrayList(1000);
        long startTime = System.currentTimeMillis();
        try (IndexWriter writer = createWriter()) {
            Files.find(rawDataPath, Integer.MAX_VALUE, this::isBook).map(book -> (Callable<Void>) () -> {
                try (InputStream in = new FileInputStream(book.toFile())) {
                    indexRecord(in, writer);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to open stream for indexing.", e);
                }
                return null;
            }).forEach(task -> {
                tasks.add(task);
                if (count.getAndIncrement() % 1000 == 0) {
                    runAndCommit(tasks, writer);
                    tasks.clear();
                }
            });

            if (!tasks.isEmpty()) {
                runAndCommit(tasks, writer);
            }

            runAndCommit(Collections.singletonList((Callable<Void>) () -> {
                Date date = new Date();
                createIndexDocument(count, date, date, writer);
                return null;
            }), writer);

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.err.println("Indexing finished in " + TimeUnit.MILLISECONDS.toMinutes(totalTime) + " minutes.");
        }
    }

    public final void addToIndex(InputStream in) throws IOException {
        try (IndexWriter writer = createWriter()) {
            indexRecord(in, writer);
            Document indexStats = getIndexDocument();
            int documentCount = Integer.valueOf(indexStats.get("document_count"));
            indexStats.removeField("document_count");
            indexStats.add(new LongField("document_count", documentCount + 1, Field.Store.YES));
            indexStats.removeField("last_update_date");
            indexStats.add(new LongField("last_update_date", new Date().getTime(), Field.Store.YES));
            writer.updateDocument(new Term("id", "index_stats"), indexStats);
            writer.commit();
        } catch (ParseException | IOException e) {
            throw new RuntimeException("Failed to open stream for indexing.", e);
        }
    }

    // -------------------- Private Methods --------------------

    private final IndexWriter createWriter() throws IOException {
        IndexWriterConfig writerConfig = new IndexWriterConfig(new StandardAnalyzer());
        writerConfig.setRAMBufferSizeMB(512d);
        writerConfig.setCommitOnClose(true);
        return new IndexWriter(FSDirectory.open(indexPath), writerConfig);
    }

    private Document getIndexDocument() throws ParseException, IOException {
        Query query = queryParser.parse("index_stats");
        DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(indexPath));
        IndexSearcher searcher = new IndexSearcher(directoryReader);
        ScoreDoc[] hits = searcher.search(query, 1).scoreDocs;
        if (hits.length == 0) {
            throw new IOException("Failed to find index document.");
        } else {
            int doc = hits[0].doc;
            return searcher.doc(doc);
        }
    }

    private void runAndCommit(List<Callable<Void>> tasks, IndexWriter writer) {
        try {
            EX.invokeAll(tasks);
            writer.commit();
        } catch (InterruptedException e) {
            throw new RuntimeException("Indexing interrupted.", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to index.", e);
        }
    }

    private boolean isBook(Path path, BasicFileAttributes attrs) {
        return !attrs.isDirectory() && path.getFileName().toString().endsWith("txt");
    }

    private void createIndexDocument(AtomicInteger count, Date creationDate, Date updateDate, IndexWriter writer) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("id", "index_stats", Field.Store.YES));
        doc.add(new LongField("creation_date", creationDate.getTime(), Field.Store.YES));
        doc.add(new LongField("last_update_date", updateDate.getTime(), Field.Store.YES));
        doc.add(new LongField("document_count", count.get(), Field.Store.YES));
        writer.addDocument(doc);
    }

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
        return "Unknown";
    }

    // -------------------- Inner Classes --------------------

    private static final class Book {
        String author = "Unknown";
        String title = "Unknown";
        String content = "";
    }

}
