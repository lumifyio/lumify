package com.altamiracorp.lumify.core.ingest.term.extraction;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TermExtractionResult {
    private final List<TermMention> termMentions = Lists.newArrayList();
    private final List<TermRelationship> relationships = Lists.newArrayList();

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

    public void addAllRelationships(List<TermRelationship> relationships) {
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

    public List<TermRelationship> getRelationships() {
        return this.relationships;
    }

    public void replace(final TermMention orig, final TermMention updated) {
        checkNotNull(orig);
        checkNotNull(updated);

        if (termMentions.remove(orig)) {
            termMentions.add(updated);
            Map<Integer, TermRelationship> relUpdates = new HashMap<Integer, TermRelationship>();
            TermRelationship rel;
            TermMention src;
            TermMention dest;
            boolean replace;
            for (int idx = 0; idx < relationships.size(); idx++) {
                rel = relationships.get(idx);
                replace = false;
                src = rel.getSourceTermMention();
                dest = rel.getDestTermMention();
                if (orig.equals(src)) {
                    src = updated;
                    replace = true;
                }
                if (orig.equals(dest)) {
                    dest = updated;
                    replace =true;
                }
                if (replace) {
                    relUpdates.put(idx, new TermRelationship(src, dest, rel.getLabel()));
                }
            }
            for (Map.Entry<Integer, TermRelationship> update : relUpdates.entrySet()) {
                relationships.remove(update.getKey().intValue());
                relationships.add(update.getKey(), update.getValue());
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + (this.termMentions != null ? this.termMentions.hashCode() : 0);
        hash = 43 * hash + (this.relationships != null ? this.relationships.hashCode() : 0);
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
        final TermExtractionResult other = (TermExtractionResult) obj;
        if (this.termMentions != other.termMentions && (this.termMentions == null || !this.termMentions.equals(other.termMentions))) {
            return false;
        }
        if (this.relationships != other.relationships && (this.relationships == null || !this.relationships.equals(other.relationships))) {
            return false;
        }
        return true;
    }
}
