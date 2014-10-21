package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Footer {
    public static final String POSITION_HEADER_FOOTER_POSITION_CENTER = "HeaderFooterPositionCenter";

    @JacksonXmlProperty(isAttribute = true)
    private String position;

    @JacksonXmlProperty(isAttribute = true)
    private String property;

    @JacksonXmlProperty(isAttribute = true)
    private boolean visible;

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
