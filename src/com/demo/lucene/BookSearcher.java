package com.demo.lucene;

import com.demo.web.IndexStats;
import com.demo.web.SearchResult;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
public final class BookSearcher {

    // -------------------- Private Statics --------------------

    private static final int MAX_HITS = 50;

    // -------------------- Private Variables --------------------

    private final QueryParser queryParser;
    private final Directory directory;

    // -------------------- Constructor --------------------

    public BookSearcher(Path indexPath) throws IOException {
        this.directory = FSDirectory.open(indexPath);
        this.queryParser = new QueryParser("contents", new StandardAnalyzer());
        this.queryParser.setAllowLeadingWildcard(true);
    }

    // -------------------- Public Methods --------------------

    public final IndexStats getIndexStats() throws ParseException, IOException {
        Query query = queryParser.parse("id:index_stats");
        IndexSearcher searcher = createSearcher();
        ScoreDoc[] hits = searcher.search(query, 1).scoreDocs;
        if (hits.length != 1) {
            throw new ParseException("Failed to get index stats from lucene.");
        }
        Document doc = searcher.doc(hits[0].doc);
        long createdDate = Long.valueOf(doc.get("creation_date"));
        long lastUpdateDate = Long.valueOf(doc.get("last_update_date"));
        int documentCount = Integer.valueOf(doc.get("document_count"));
        return new IndexStats(documentCount, lastUpdateDate, createdDate);
    }

    public final Set<SearchResult> search(String searchText) throws ParseException, IOException, InvalidTokenOffsetsException {
        String queryText = "\"*" + QueryParser.escape(searchText.toLowerCase()) + "*\"";
        Query query = queryParser.parse(queryText);
        IndexSearcher searcher = createSearcher();
        ScoreDoc[] hits = searcher.search(query, MAX_HITS).scoreDocs;
        Highlighter highlighter = new Highlighter(new QueryScorer(query));

        Set<SearchResult> results = new LinkedHashSet<>();
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document doc = searcher.doc(docId);
            TokenStream tokenStream = getTokenStream(docId, searcher);
            TextFragment[] fragments = highlighter.getBestTextFragments(tokenStream, doc.get("contents"), true, 1);
            String context = fragments.length == 1 ? fragments[0].toString() : "";
            if (!context.trim().isEmpty()) {
                String author = doc.get("author");
                String title = doc.get("title");
                results.add(new SearchResult(author, title, context));
            }
        }
        return results;
    }

    // -------------------- Private Methods --------------------

    private IndexSearcher createSearcher() throws IOException {
        DirectoryReader directoryReader = DirectoryReader.open(directory);
        return new IndexSearcher(directoryReader);
    }

    @SuppressWarnings("deprecation")
    private TokenStream getTokenStream(int docId, IndexSearcher searcher) throws IOException {
        return TokenSources.getAnyTokenStream(
                searcher.getIndexReader(),
                docId,
                "contents",
                new StandardAnalyzer()
        );
    }
}
