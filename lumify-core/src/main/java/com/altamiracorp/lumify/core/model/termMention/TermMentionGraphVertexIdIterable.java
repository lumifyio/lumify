package com.altamiracorp.lumify.core.model.termMention;

import com.altamiracorp.securegraph.util.LookAheadIterable;

import java.util.Iterator;

public class TermMentionGraphVertexIdIterable extends LookAheadIterable<TermMentionModel, Object> {
    private Iterable<TermMentionModel> termMentions;

    public TermMentionGraphVertexIdIterable(Iterable<TermMentionModel> termMentions) {
        this.termMentions = termMentions;
    }

    @Override
    protected boolean isIncluded(TermMentionModel termMentionModel, Object o) {
        return o != null;
    }

    @Override
    protected Object convert(TermMentionModel termMentionModel) {
        return termMentionModel.getMetadata().getGraphVertexId();
    }

    @Override
    protected Iterator<TermMentionModel> createIterator() {
        return termMentions.iterator();
    }
}
