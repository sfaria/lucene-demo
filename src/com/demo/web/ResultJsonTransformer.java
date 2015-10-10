package com.demo.web;

import com.demo.lucene.SearchResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import spark.ResponseTransformer;

import java.util.Collection;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
final class ResultJsonTransformer implements ResponseTransformer {

    // -------------------- Private Variables --------------------

    private final Gson gson = new Gson();

    // -------------------- Overridden --------------------

    @Override
    public final String render(Object model) throws Exception {
        Collection<SearchResult> results = (Collection<SearchResult>) model;
        JsonArray docArray = new JsonArray();
        results.forEach(result -> docArray.add(result.toJson()));
        return gson.toJson(docArray);
    }
}
