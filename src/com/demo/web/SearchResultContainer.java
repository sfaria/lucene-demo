package com.demo.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collection;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
final class SearchResultContainer implements Jsonable {

    // -------------------- Private Variables --------------------

    private final long elapsedTime;
    private final Collection<SearchResult> results;

    // -------------------- Constructor --------------------

    SearchResultContainer(long elapsedTime, Collection<SearchResult> results) {
        this.elapsedTime = elapsedTime;
        this.results = results;
    }

    // -------------------- Overridden Methods --------------------

    @Override
    public final JsonElement toJson() {
        JsonObject containerJson = new JsonObject();
        containerJson.addProperty("elapsed_time", elapsedTime);
        JsonArray docArray = new JsonArray();
        containerJson.add("results", docArray);
        results.forEach(result -> docArray.add(result.toJson()));
        return containerJson;
    }

}
