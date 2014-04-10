package com.altamiracorp.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TermMentionRepository extends Repository<TermMentionModel> {
    private TermMentionBuilder termMentionBuilder = new TermMentionBuilder();

    @Inject
    public TermMentionRepository(final ModelSession modelSession) {
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

    public Iterable<TermMentionModel> findByGraphVertexId(String graphVertexId, ModelUserContext modelUserContext) {
        return findByRowStartsWith(graphVertexId + TermMentionRowKey.ROW_KEY_SEP, modelUserContext);
    }
}
