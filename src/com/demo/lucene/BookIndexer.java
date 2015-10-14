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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
public final class BookIndexer {

    // -------------------- Private Variables --------------------

    private final Path rawDataPath;
    private final Path indexPath;

    // -------------------- Constructors --------------------

    public BookIndexer(Path rawDataPath, Path indexPath) {
        this.rawDataPath = rawDataPath;
        this.indexPath = indexPath;
    }

    // -------------------- Public Methods --------------------

    public final void performFullIndexing() throws IOException {
        System.err.println("Performing a full indexing of all books...");
        long startTime = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);

        try (IndexWriter writer = createWriter()) {
            Stream.of(rawDataPath.toFile().listFiles(BookParser::isBook))
                    .parallel()
                    .map(this::newInputStream)
                    .forEach(in -> {
                        indexRecord(in, writer);
                        maybeCommit(count, writer);
                    });

            Date date = new Date();
            createIndexDocument(count, date, date, writer);
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.err.println("Indexing finished in " + TimeUnit.MILLISECONDS.toSeconds(totalTime) + "s.");
    }

    public final void addToIndex(InputStream in) throws IOException {
        try (IndexWriter writer = createWriter()) {
            indexRecord(in, writer);
            updateIndexStatistics(writer);
        }
    }

    // -------------------- Private Methods --------------------

    private InputStream newInputStream(File f) {
        try {
            return new FileInputStream(f);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to open book.", e);
        }
    }

    private void maybeCommit(AtomicInteger count, IndexWriter writer)  {
        try {
            if (count.incrementAndGet() % 1000 == 0) {
                writer.commit();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to index books.", e);
        }
    }

    private final IndexWriter createWriter() throws IOException {
        IndexWriterConfig writerConfig = new IndexWriterConfig(new StandardAnalyzer());
        writerConfig.setRAMBufferSizeMB(512d);
        writerConfig.setCommitOnClose(true);
        return new IndexWriter(FSDirectory.open(indexPath), writerConfig);
    }

    private Document getIndexDocument() throws IOException {
        DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(indexPath));
        IndexSearcher searcher = new IndexSearcher(directoryReader);
        ScoreDoc[] hits = searcher.search(new TermQuery(new Term("id", "index_stats")), 1).scoreDocs;
        if (hits == null || hits.length == 0) {
            throw new IOException("Failed to find index document.");
        } else {
            return searcher.doc(hits[0].doc);
        }
    }

    private void updateIndexStatistics(IndexWriter writer) throws IOException {
        Document indexStats = getIndexDocument();
        int documentCount = Integer.valueOf(indexStats.get("document_count"));
        indexStats.removeField("document_count");
        indexStats.add(new LongField("document_count", documentCount + 1, Field.Store.YES));
        indexStats.removeField("last_update_date");
        indexStats.add(new LongField("last_update_date", new Date().getTime(), Field.Store.YES));
        writer.updateDocument(new Term("id", "index_stats"), indexStats);
    }

    private void createIndexDocument(AtomicInteger count, Date creationDate, Date updateDate, IndexWriter writer) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("id", "index_stats", Field.Store.YES));
        doc.add(new LongField("creation_date", creationDate.getTime(), Field.Store.YES));
        doc.add(new LongField("last_update_date", updateDate.getTime(), Field.Store.YES));
        doc.add(new LongField("document_count", count.get(), Field.Store.YES));
        writer.addDocument(doc);
    }

    private void indexRecord(InputStream in, IndexWriter writer) {
        Document doc = new Document();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            Book book = BookParser.parse(reader);
            doc.add(new LongField("created", new Date().getTime(), Field.Store.YES));
            doc.add(new StringField("author", book.getAuthor(), Field.Store.YES));
            doc.add(new StringField("title", book.getTitle(), Field.Store.YES));
            doc.add(new TermVectorEnabledTextField("contents", book.getContent()));
            writer.addDocument(doc);
        } catch (IOException e) {
            throw new RuntimeException("Failed add book to the index.", e);
        }
    }



}
