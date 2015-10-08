package com.demo.web;

import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.get;
import static spark.Spark.port;

/**
 * @author Scott Faria
 */
public final class BookSearchServer {

    // -------------------- Main --------------------

    public static void main(String[] args) {
        port(9090);
        externalStaticFileLocation("client/libs");

        get("/", (request, response) -> {
            return "Hello, World!";
        });
    }
}
