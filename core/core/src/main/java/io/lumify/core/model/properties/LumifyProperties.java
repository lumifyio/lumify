package io.lumify.core.model.properties;

import io.lumify.core.model.properties.types.DateLumifyProperty;
import io.lumify.core.model.properties.types.DoubleLumifyProperty;
import io.lumify.core.model.properties.types.StreamingLumifyProperty;
import io.lumify.core.model.properties.types.StringLumifyProperty;

public class LumifyProperties {
    public static final StringLumifyProperty DISPLAY_NAME = new StringLumifyProperty("http://lumify.io#displayName");
    public static final StringLumifyProperty PROCESS = new StringLumifyProperty("http://lumify.io#process");
    public static final StringLumifyProperty ROW_KEY = new StringLumifyProperty("http://lumify.io#rowKey");
    public static final StringLumifyProperty CONTENT_HASH = new StringLumifyProperty("http://lumify.io#contentHash");
    public static final StreamingLumifyProperty GLYPH_ICON = new StreamingLumifyProperty("http://lumify.io#glyphIcon");
    public static final StreamingLumifyProperty MAP_GLYPH_ICON = new StreamingLumifyProperty("http://lumify.io#mapGlyphIcon");
    public static final StringLumifyProperty TITLE = new StringLumifyProperty("http://lumify.io#title");
    public static final DateLumifyProperty MODIFIED_DATE = new DateLumifyProperty("http://lumify.io#modifiedDate");
    public static final StringLumifyProperty MODIFIED_BY = new StringLumifyProperty("http://lumify.io#modifiedBy");
    public static final DoubleLumifyProperty CONFIDENCE = new DoubleLumifyProperty("http://lumify.io#confidence");

    private LumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
