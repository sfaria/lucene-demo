package com.demo.web;

import com.google.gson.JsonElement;

/**
 * @author Scott Faria
 */
public interface Jsonable {
    JsonElement toJson();
}
