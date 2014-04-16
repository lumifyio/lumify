package com.altamiracorp.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.ingest.term.extraction.TermMention;
import com.altamiracorp.lumify.core.model.audit.Audit;
import com.google.inject.Inject;

public class BigTableTermMentionRepository extends TermMentionRepository {
    private TermMentionBuilder termMentionBuilder = new TermMentionBuilder();

    @Inject
    public BigTableTermMentionRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public TermMentionModel fromRow(Row row) {
        return termMentionBuilder.fromRow(row);
    }

    @Override
    public Row toRow(TermMentionModel obj) {
        return obj;
    }

    @Override
    public String getTableName() {
        return termMentionBuilder.getTableName();
    }

    @Override
    public Iterable<TermMentionModel> findByGraphVertexId(String graphVertexId, ModelUserContext modelUserContext) {
        return findByRowStartsWith(graphVertexId + TermMentionRowKey.ROW_KEY_SEP, modelUserContext);
    }

    @Override
    public void updateColumnVisibility(TermMentionModel termMentionModel, String originalEdgeVisibility, String visibilityString, FlushFlag flushFlag) {
        getModelSession().alterColumnsVisibility(termMentionModel, originalEdgeVisibility, visibilityString, flushFlag);
    }
}
