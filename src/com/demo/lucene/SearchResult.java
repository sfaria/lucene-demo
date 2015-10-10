package com.demo.lucene;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
public final class SearchResult {
    // -------------------- Private Variables --------------------

    private final String author;
    private final String title;
    private final String contextMatch;

    // -------------------- Constructors --------------------

    public SearchResult(String author, String title, String contextMatch) {
        this.author = author;
        this.title = title;
        this.contextMatch = contextMatch;
    }

    // -------------------- Public Methods --------------------

    public final JsonElement toJson() {
        JsonObject matchesObject = new JsonObject();
        matchesObject.addProperty("author", author);
        matchesObject.addProperty("title", title);
        matchesObject.addProperty("context", contextMatch);
        return matchesObject;
    }

    // -------------------- Overridden Methods --------------------


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
        if (!author.equals(that.author)) return false;
        return title.equals(that.title);

    }

    @Override
    public int hashCode() {
        int result = author.hashCode();
        result = 31 * result + title.hashCode();
        return result;
    }
}
