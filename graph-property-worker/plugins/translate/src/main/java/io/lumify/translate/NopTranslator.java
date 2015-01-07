package io.lumify.translate;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;

public class NopTranslator implements Translator {
    @Override
    public String translate(String text, String language, GraphPropertyWorkData data) {
        return null;
    }
}
