package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class LinkType {
    public static final String NAME_LINK = "Link";

    @JacksonXmlProperty(isAttribute = true)
    private String colour;

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
