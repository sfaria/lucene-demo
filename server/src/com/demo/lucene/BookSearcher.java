package com.demo.lucene;

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

    public final List<SearchResult> search(String searchText) throws ParseException, IOException, InvalidTokenOffsetsException {
        Query query = queryParser.parse(searchText);
        ScoreDoc[] hits = searcher.search(query, MAX_HITS).scoreDocs;
        Highlighter highlighter = new Highlighter(new QueryScorer(query));

        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < hits.length; i++) {
            ScoreDoc hit = hits[i];
            int docId = hit.doc;
            Document doc = searcher.doc(docId);
            String text = doc.get("content");
            TokenStream tokenStream = getTokenStream(docId);
            TextFragment[] fragments = highlighter.getBestTextFragments(tokenStream, text, false, 5);
            List<String> matches = new ArrayList<>();
            for (int j = 0; j < fragments.length; j++) {
                TextFragment fragment = fragments[j];
                if (fragment != null && fragment.getScore() > 0) {
                    matches.add(fragment.toString());
                }
            }
            results.add(new SearchResult(matches));
        }
        return results;
    }

    // -------------------- Private Methods --------------------

    @SuppressWarnings("deprecation")
    private TokenStream getTokenStream(int docId) throws IOException {
        return TokenSources.getAnyTokenStream(
                searcher.getIndexReader(),
                docId,
                "content",
                new StandardAnalyzer()
        );
    }
}
