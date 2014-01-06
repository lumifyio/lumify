package com.altamiracorp.lumify.core.model.search;

import java.util.Date;

public class ArtifactSearchResult {
    private final String source;
    private final String rowKey;
    private final String subject;
    private final Date publishedDate;
    private final String graphVertexId;

    public ArtifactSearchResult(String rowKey, String subject, Date publishedDate, String source, String graphVertexId) {
        this.rowKey = rowKey;
        this.subject = subject;
        this.publishedDate = publishedDate;
        this.source = source;
        this.graphVertexId = graphVertexId;
    }

    public String getRowKey() {
        return rowKey;
    }

    public String getSubject() {
        return subject;
    }

    public Date getPublishedDate() {
        return this.publishedDate;
    }

    public String getSource() {
        return this.source;
    }

    public String getGraphVertexId() {
        return graphVertexId;
    }

    @Override
    public String toString() {
        return "rowKey: " + getRowKey() + ", subject: " + getSubject() + ", publishedDate: " + getPublishedDate() + ", source: " + getSource() + ", graphVertexId: " + getGraphVertexId();
    }
}
