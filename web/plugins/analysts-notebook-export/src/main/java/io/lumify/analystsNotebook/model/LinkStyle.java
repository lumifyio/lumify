package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class LinkStyle {
    public static final String ARROW_STYLE_ARROW_NONE = "ArrowNone";
    public static final String ARROW_STYLE_ARROW_ON_BOTH = "ArrowOnBoth";
    public static final String ARROW_STYLE_ARROW_ON_HEAD = "ArrowOnHead";
    public static final String ARROW_STYLE_ARROW_ON_TAIL = "ArrowOnTail";
    public static final String TYPE_LINK = "Link";

    // present only in version 6
    @JacksonXmlProperty(isAttribute = true)
    private Integer strength;

    @JacksonXmlProperty(isAttribute = true)
    private String arrowStyle;

    @JacksonXmlProperty(isAttribute = true)
    private String type;

    public Integer getStrength() {
        return strength;
    }

    public void setStrength(Integer strength) {
        this.strength = strength;
    }

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
