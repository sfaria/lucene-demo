package com.demo.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
public final class BookSearcher {

    // -------------------- Private Statics --------------------

    private static final int MAX_HITS = 50;

    // -------------------- Private Variables --------------------

    private final IndexSearcher searcher;
    private final QueryParser queryParser;

    // -------------------- Constructor --------------------

    public BookSearcher(Path indexPath) throws IOException {
        Directory directory = FSDirectory.open(indexPath);
        DirectoryReader directoryReader = DirectoryReader.open(directory);
        this.searcher = new IndexSearcher(directoryReader);
        this.queryParser = new QueryParser("content", new StandardAnalyzer());
    }

    // -------------------- Public Methods --------------------

    public final List<Document> search(String searchText) throws ParseException, IOException {
        ScoreDoc[] hits = searcher.search(queryParser.parse(searchText), MAX_HITS).scoreDocs;
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < hits.length; i++) {
            ScoreDoc hit = hits[i];
            docs.add(searcher.doc(hit.doc));
        }
        return docs;
    }
}
