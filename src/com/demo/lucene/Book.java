package com.demo.lucene;

import static spark.utils.StringUtils.isEmpty;

/**
 * @author Scott Faria
 */
final class Book {

    // -------------------- Private Variables --------------------

    private final String author;
    private final String title;
    private final String content;

    // -------------------- Constructors --------------------

    Book(String author, String title, String content) {
        this.author = isEmpty(author) ? "Unknown" : author;
        this.title = isEmpty(title) ? "Unknown" : title;
        this.content = content == null ? "" : content;
    }

    // -------------------- Default Methods --------------------

    final String getAuthor() {
        return author;
    }

    final String getTitle() {
        return title;
    }

    final String getContent() {
        return content;
    }

}
