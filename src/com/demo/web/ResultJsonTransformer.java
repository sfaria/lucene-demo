package com.demo.web;

import com.google.gson.Gson;
import spark.ResponseTransformer;

/**
 * @author Scott Faria <scott.faria@gmail.com>
 */
final class ResultJsonTransformer implements ResponseTransformer {

    // -------------------- Private Variables --------------------

    private final Gson gson = new Gson();

    // -------------------- Overridden --------------------

    @Override
    public final String render(Object model) throws Exception {
        return gson.toJson(((SearchResultContainer) model).toJson());
    }
}
