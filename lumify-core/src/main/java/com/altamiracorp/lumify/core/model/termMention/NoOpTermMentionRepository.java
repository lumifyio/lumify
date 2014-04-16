package com.altamiracorp.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.beust.jcommander.internal.Nullable;
import com.google.inject.Inject;

public class NoOpTermMentionRepository extends TermMentionRepository {
    @Inject
    public NoOpTermMentionRepository(@Nullable ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public TermMentionModel fromRow(Row row) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Row toRow(TermMentionModel obj) {
        throw new RuntimeException("not supported");
    }

    @Override
    public String getTableName() {
        throw new RuntimeException("not supported");
    }

    @Override
    public Iterable<TermMentionModel> findByGraphVertexId(String graphVertexId, ModelUserContext modelUserContext) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void updateColumnVisibility(TermMentionModel termMentionModel, String originalEdgeVisibility, String visibilityString, FlushFlag flushFlag) {
        throw new RuntimeException("not supported");
    }
}
