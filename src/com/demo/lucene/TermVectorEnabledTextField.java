package com.demo.lucene;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

/**
 * @author Scott Faria
 */
final class TermVectorEnabledTextField extends Field {

    // -------------------- Statics --------------------

    private static final FieldType TYPE = new FieldType();
    static {
        TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        TYPE.setTokenized(true);
        TYPE.setStored(true);
        TYPE.setStoreTermVectors(true);
        TYPE.setStoreTermVectorOffsets(true);
        TYPE.setStoreTermVectorPositions(true);
        TYPE.freeze();
    }

    // -------------------- Constructors --------------------

    public TermVectorEnabledTextField(String name, String value) {
        super(name, value, TYPE);
    }

}
