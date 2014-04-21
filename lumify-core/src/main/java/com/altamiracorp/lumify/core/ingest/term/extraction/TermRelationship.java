package com.altamiracorp.lumify.core.ingest.term.extraction;

import com.altamiracorp.securegraph.Visibility;

/**
 * A relationship between two mentioned terms.
 */
public class TermRelationship {
    private final TermMention sourceTermMention;
    private final TermMention destTermMention;
    private final String label;
    private final Visibility visibility;

    public TermRelationship(TermMention sourceTermMention, TermMention destTermMention, String label, Visibility visibility) {
        this.sourceTermMention = sourceTermMention;
        this.destTermMention = destTermMention;
        this.label = label;
        this.visibility = visibility;
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

    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this.sourceTermMention != null ? this.sourceTermMention.hashCode() : 0);
        hash = 13 * hash + (this.destTermMention != null ? this.destTermMention.hashCode() : 0);
        hash = 13 * hash + (this.label != null ? this.label.hashCode() : 0);
        hash = 13 * hash + (this.visibility != null ? this.visibility.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TermRelationship other = (TermRelationship) obj;
        if (this.sourceTermMention != other.sourceTermMention && (this.sourceTermMention == null || !this.sourceTermMention.equals(other.sourceTermMention))) {
            return false;
        }
        if (this.destTermMention != other.destTermMention && (this.destTermMention == null || !this.destTermMention.equals(other.destTermMention))) {
            return false;
        }
        if ((this.label == null) ? (other.label != null) : !this.label.equals(other.label)) {
            return false;
        }
        if ((this.visibility == null) ? (other.visibility != null) : !this.visibility.equals(other.visibility)) {
            return false;
        }
        return true;
    }
}
