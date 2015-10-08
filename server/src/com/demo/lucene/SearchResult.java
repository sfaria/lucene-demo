package com.demo.lucene;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
public final class SearchResult {

    // -------------------- Private Variables --------------------

    private final List<String> matches;

    // -------------------- Constructors --------------------

    public SearchResult(List<String> matches) {
        this.matches = matches;
    }

    // -------------------- Public Methods --------------------

    public final JsonElement toJson() {
        JsonArray array = new JsonArray();
        JsonObject matchesObject = new JsonObject();
        matchesObject.add("matches", array);
        matches.forEach(array::add);
        return matchesObject;
    }
}
