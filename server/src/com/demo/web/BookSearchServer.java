package com.demo.web;

import com.demo.lucene.BookIndexer;
import com.demo.lucene.BookSearcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    public static void main(String[] args) throws Exception {
        // set up lucene objects
        BookSearcher searcher = new BookSearcher(INDEX_PATH);
        BookIndexer indexer = new BookIndexer(RAW_DATA_PATH, INDEX_PATH);
        indexer.performFullIndexing();

        // configure spark
        port(9090);
        externalStaticFileLocation("client/libs");

        // server routes
        exception(Exception.class, (e, request, response) -> {
            response.status(500);
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            response.body(stringWriter.toString());
        });

        get("/", (request, response) -> {
            byte[] encoded = Files.readAllBytes(Paths.get("client", "index.html"));
            return new String(encoded, StandardCharsets.UTF_8);
        });

        get("/search/:searchText", (request, response) -> {
            return searcher.search(request.params(":searchText"));
        }, new ResultJsonTransformer());

    }
}
