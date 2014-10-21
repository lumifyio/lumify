package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class PrintSettings {
    @JacksonXmlElementWrapper(localName = "HeaderCollection")
    @JacksonXmlProperty(localName = "Header")
    private List<Header> headerCollection;

    @JacksonXmlElementWrapper(localName = "FooterCollection")
    @JacksonXmlProperty(localName = "Footer")
    private List<Footer> footerCollection;

    public List<Header> getHeaderCollection() {
        return headerCollection;
    }

    public void setHeaderCollection(List<Header> headerCollection) {
        this.headerCollection = headerCollection;
    }

    public List<Footer> getFooterCollection() {
        return footerCollection;
    }

    public void setFooterCollection(List<Footer> footerCollection) {
        this.footerCollection = footerCollection;
    }
}
