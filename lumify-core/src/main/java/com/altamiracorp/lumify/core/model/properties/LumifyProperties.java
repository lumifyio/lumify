package com.altamiracorp.lumify.core.model.properties;

import com.altamiracorp.lumify.core.model.properties.types.DateLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.types.StreamingLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.types.TextLumifyProperty;
import com.altamiracorp.securegraph.TextIndexHint;

public class LumifyProperties {
    public static final TextLumifyProperty DISPLAY_NAME = TextLumifyProperty.all("http://lumify.io#displayName");
    public static final TextLumifyProperty PROCESS = TextLumifyProperty.all("http://lumify.io#process");
    public static final TextLumifyProperty ROW_KEY = new TextLumifyProperty("http://lumify.io#rowKey", TextIndexHint.EXACT_MATCH);
    public static final StreamingLumifyProperty GLYPH_ICON = new StreamingLumifyProperty("http://lumify.io#glyphIcon");
    public static final StreamingLumifyProperty MAP_GLYPH_ICON = new StreamingLumifyProperty("http://lumify.io#mapGlyphIcon");
    public static final TextLumifyProperty TITLE = TextLumifyProperty.all("http://lumify.io#title");
    public static final DateLumifyProperty MODIFIED_DATE = new DateLumifyProperty("http://lumify.io#modifiedDate");
    public static final TextLumifyProperty MODIFIED_BY = new TextLumifyProperty("http://lumify.io#modifiedBy", TextIndexHint.EXACT_MATCH);

    private LumifyProperties() {
        throw new UnsupportedOperationException("do not construct utility class");
    }
}
