package com.demo.web;

import com.demo.lucene.BookIndexer;
import com.demo.lucene.BookSearcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static spark.Spark.*;

/**
 * @author Scott Faria
 */
public final class BookSearchServer {

    // -------------------- Private Statics --------------------

    private static final Path RAW_DATA_PATH = Paths.get("books");
    private static final Path INDEX_PATH = Paths.get("index");

    // -------------------- Main --------------------

    public static void main(String[] args) throws IOException {
        // set up lucene objects
        BookSearcher searcher = new BookSearcher(INDEX_PATH);
        BookIndexer indexer = new BookIndexer(RAW_DATA_PATH, INDEX_PATH);
        indexer.performFullIndexing();

        // configure spark
        port(9090);
        externalStaticFileLocation("client/libs");

        // server routes
        get("/", (request, response) -> {
            return "Hello, World!";
        });

        
    }
}
