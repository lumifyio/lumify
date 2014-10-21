package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class LinkStyle {
    public static final String ARROW_STYLE_ARROW_NONE = "ArrowNone";
    public static final String TYPE_LINK = "Link";

    @JacksonXmlProperty(isAttribute = true)
    private String arrowStyle;

    @JacksonXmlProperty(isAttribute = true)
    private String type;

    public String getArrowStyle() {
        return arrowStyle;
    }

    public void setArrowStyle(String arrowStyle) {
        this.arrowStyle = arrowStyle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
