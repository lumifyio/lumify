package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Link {
    @JacksonXmlProperty(isAttribute = true)
    private String end1Id;

    @JacksonXmlProperty(isAttribute = true)
    private String end2Id;

    private LinkStyle linkStyle;

    public String getEnd1Id() {
        return end1Id;
    }

    public void setEnd1Id(String end1Id) {
        this.end1Id = end1Id;
    }

    public String getEnd2Id() {
        return end2Id;
    }

    public void setEnd2Id(String end2Id) {
        this.end2Id = end2Id;
    }

    public LinkStyle getLinkStyle() {
        return linkStyle;
    }

    public void setLinkStyle(LinkStyle linkStyle) {
        this.linkStyle = linkStyle;
    }
}
