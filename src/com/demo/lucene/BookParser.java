package com.demo.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * @author Scott Faria
 */
final class BookParser {

    // -------------------- Default Static Methods --------------------

    static boolean isBook(File dir, String name) {
        return !new File(dir, name).isDirectory() && name.endsWith("txt");
    }

    static Book parse(BufferedReader reader) throws IOException {
        String author = null;
        String title = null;

        boolean hitEndHeader = false;
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                if (!hitEndHeader) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.startsWith("***") && trimmedLine.endsWith("***")) {
                        hitEndHeader = true;
                    } else {
                        if (trimmedLine.toLowerCase().startsWith("author:")) {
                            author = parseHeaderAttribute(trimmedLine);
                        } else if (trimmedLine.toLowerCase().startsWith("title:")) {
                            title = parseHeaderAttribute(trimmedLine);
                        }
                    }
                } else {
                    sb.append(line.toLowerCase().trim()).append(" ");
                }
            }
        }

        return new Book(author, title, sb.toString());
    }

    // -------------------- Private Static Methods --------------------

    private static String parseHeaderAttribute(String line) {
        String[] split = line.split(":");
        if (split.length == 2) {
            return split[1].trim();
        }
        return null;
    }

    // -------------------- Constructors --------------------

    private BookParser() {}
}
