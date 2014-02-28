package com.altamiracorp.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import com.altamiracorp.securegraph.Visibility;

public class TermMentionMetadata extends ColumnFamily {
    public static final String NAME = "Metadata";
    public static final String SIGN = "sign";
    public static final String ONTOLOGY_CLASS_URI = "ontologyClassUri";
    public static final String VERTEX_ID = "graphVertexId";
    public static final String CONCEPT_GRAPH_VERTEX_ID = "conceptGraphVertexId";
    public static final String ANALYTIC_PROCESS = "analyticProcess";

    public TermMentionMetadata() {
        super(NAME);
    }

    public TermMentionMetadata setSign(String text, Visibility visibility) {
        set(SIGN, text, visibility.getVisibilityString());
        return this;
    }

    public String getSign() {
        return Value.toString(get(SIGN));
    }

    public TermMentionMetadata setVertexId(String vertexId, Visibility visibility) {
        set(VERTEX_ID, vertexId, visibility.getVisibilityString());
        return this;
    }

    public String getGraphVertexId() {
        return Value.toString(get(VERTEX_ID));
    }

    public TermMentionMetadata setOntologyClassUri(String ontologyClassUri, Visibility visibility) {
        set(ONTOLOGY_CLASS_URI, ontologyClassUri, visibility.getVisibilityString());
        return this;
    }

    public String getOntologyClassUri() {
        return Value.toString(get(ONTOLOGY_CLASS_URI));
    }

    public TermMentionMetadata setConceptGraphVertexId(Object conceptGraphVertexId, Visibility visibility) {
        set(CONCEPT_GRAPH_VERTEX_ID, conceptGraphVertexId, visibility.getVisibilityString());
        return this;
    }

    public String getConceptGraphVertexId() {
        return Value.toString(get(CONCEPT_GRAPH_VERTEX_ID));
    }

    public String getAnalyticProcess() {
        return Value.toString(get(ANALYTIC_PROCESS));
    }

    public TermMentionMetadata setAnalyticProcess(String analyticProcess, Visibility visibility) {
        set(ANALYTIC_PROCESS, analyticProcess, visibility.getVisibilityString());
        return this;
    }
}
