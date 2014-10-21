package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IconStyle {
    @JacksonXmlProperty(isAttribute = true)
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
