package com.demo.web;

import com.demo.lucene.BookIndexer;
import com.demo.lucene.BookSearcher;
import com.demo.lucene.SearchResult;
import spark.Spark;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

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
        // configure spark
        staticFileLocation("/WEB-INF");
        port(9090);

        // set up lucene objects
        BookSearcher searcher = new BookSearcher(INDEX_PATH);
        BookIndexer indexer = new BookIndexer(RAW_DATA_PATH, INDEX_PATH);

        if (performStartupIndexing(args)) {
            indexer.performFullIndexing();
        }

        // server routes
        exception(Exception.class, (e, request, response) -> {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            e.printStackTrace(System.err);

            response.status(500);
            response.type("text/html");
            response.body(
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<body>\n" +
            "   <h2>500: Something went way, way wrong!</h2>\n" +
            "   <pre><code>\n"
                    + stringWriter.toString() + "\n" +
            "   </code></pre>\n" +
            "</body>\n" +
            "</html>"
            );
        });

        get("/", "text/html", (request, response) -> {
            File indexPage = new File(Spark.class.getResource("/WEB-INF/index.html").toURI());
            byte[] encoded = Files.readAllBytes(indexPage.toPath());
            return new String(encoded, StandardCharsets.UTF_8);
        });

        get("/search/:searchText", "application/json", (request, response) -> {
            long startTime = System.currentTimeMillis();
            Set<SearchResult> searchResults = searcher.search(request.params(":searchText"));
            long endTime = System.currentTimeMillis();
            long elapsedTime = Math.max(0L, endTime - startTime);
            return new SearchResultContainer(elapsedTime, searchResults);
        }, new ResultJsonTransformer());

    }

    // -------------------- Private Methods --------------------

    private static boolean performStartupIndexing(String[] args) {
        if (args.length == 1) {
            String[] arg = args[0].split("=");
            if (arg[0].equals("indexAtStartup")) {
                return Boolean.parseBoolean(arg[1]);
            }
        }
        return true;
    }
}
