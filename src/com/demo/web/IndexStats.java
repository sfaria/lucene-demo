package com.demo.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Scott Faria
 */
public class IndexStats implements Jsonable {

    // -------------------- Private Statics --------------------


    // -------------------- Private Variables --------------------

    private final int documentCount;
    private final Date lastUpdateDate;
    private final Date creationDate;

    private final SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    // -------------------- Constructors --------------------

    public IndexStats(int documentCount, long lastUpdateDate, long creationDate) {
        this.documentCount = documentCount;
        this.lastUpdateDate = new Date(lastUpdateDate);
        this.creationDate = new Date(creationDate);
    }

    // -------------------- Overridden Methods --------------------

    @Override
    public final JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("document_count", documentCount);
        object.addProperty("creation_date", formatter.format(creationDate));
        object.addProperty("last_update_date", formatter.format(lastUpdateDate));
        return object;
    }
}
