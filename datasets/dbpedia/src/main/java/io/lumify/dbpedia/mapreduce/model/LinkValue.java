package io.lumify.dbpedia.mapreduce.model;

public class LinkValue extends Value {
    private final String pageIri;
    private final String pageTitle;

    public LinkValue(String pageIri) {
        this.pageIri = pageIri;
        this.pageTitle = LineData.parsePageTitleFromPageUrl(pageIri);
    }

    @Override
    public Object getValue() {
        return this.pageIri;
    }

    @Override
    public String getValueString() {
        return this.pageIri;
    }

    public String getPageTitle() {
        return pageTitle;
    }
}
