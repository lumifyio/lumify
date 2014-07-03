package io.lumify.translate;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;

public interface Translator {
    String translate(String text, String language, GraphPropertyWorkData data);
}
