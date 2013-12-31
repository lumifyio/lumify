package com.altamiracorp.lumify.core.ingest.term.extraction;

import com.altamiracorp.bigtable.model.Value;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class TermExtractionResult {
    private final List<TermMention> termMentions = Lists.newArrayList();
    private final List<Relationship> relationships = Lists.newArrayList();

    public void add(TermMention termMention) {
        checkNotNull(termMention);

        termMentions.add(termMention);
    }

    public void addAllTermMentions(List<TermMention> termMentions) {
        checkNotNull(termMentions);
        for (TermMention t : termMentions) {
            if (t != null) {
                this.termMentions.add(t);
            }
        }
    }

    public void addAllRelationships(List<Relationship> relationships) {
        checkNotNull(relationships);
        this.relationships.addAll(relationships);
    }

    public void mergeFrom(TermExtractionResult result) {
        checkNotNull(result);
        checkNotNull(result.termMentions);

        termMentions.addAll(result.termMentions);
        relationships.addAll(result.relationships);
    }

    public List<TermMention> getTermMentions() {
        return termMentions;
    }

    public List<Relationship> getRelationships() {
        return this.relationships;
    }

    // TODO: rename to a better class name
    public static class TermMention {

        private final int start;
        private final int end;
        private final String sign;
        private final String ontologyClassUri;
        private final String relationshipLabel;
        private final boolean resolved;
        private final Map<String, Object> propertyValue;
        private final boolean useExisting;
        private String process = "";

        public TermMention(int start, int end, String sign, String ontologyClassUri, boolean resolved, Map<String, Object> propertyValue, String relationshipLabel, boolean useExisting) {
            this.start = start;
            this.end = end;
            this.sign = sign;
            this.ontologyClassUri = ontologyClassUri;
            this.resolved = resolved;
            this.propertyValue = propertyValue;
            this.relationshipLabel = relationshipLabel;
            this.useExisting = useExisting;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public String getSign() {
            return sign;
        }

        public String getOntologyClassUri() {
            return ontologyClassUri;
        }

        public boolean isResolved() {
            return resolved;
        }

        public Map<String, Object> getPropertyValue() {
            return propertyValue;
        }

        public String getRelationshipLabel() {
            return relationshipLabel;
        }

        public boolean getUseExisting() {
            return useExisting;
        }

        public String getProcess () { return process; }

        public void setProcess (String process) { this.process = process; }

        public String toString () {
            return getSign() + ": " + getStart() + ": " + getEnd();
        }
    }

    public static class Relationship {
        private final TermMention sourceTermMention;
        private final TermMention destTermMention;
        private final String label;

        public Relationship(TermMention sourceTermMention, TermMention destTermMention, String label) {
            this.sourceTermMention = sourceTermMention;
            this.destTermMention = destTermMention;
            this.label = label;
        }

        public TermMention getSourceTermMention() {
            return sourceTermMention;
        }

        public TermMention getDestTermMention() {
            return destTermMention;
        }

        public String getLabel() {
            return label;
        }
    }
}
