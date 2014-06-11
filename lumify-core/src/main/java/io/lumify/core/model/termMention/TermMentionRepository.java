package io.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.google.inject.Inject;

public abstract class TermMentionRepository extends Repository<TermMentionModel> {
    @Inject
    public TermMentionRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    public abstract TermMentionModel fromRow(Row row);

    public abstract Row toRow(TermMentionModel obj);

    public abstract String getTableName();

    public abstract Iterable<TermMentionModel> findByGraphVertexIdAndPropertyKey(String graphVertexId, String propertyKey, ModelUserContext modelUserContext);

    public abstract void updateColumnVisibility(TermMentionModel termMentionModel, String originalEdgeVisibility, String visibilityString, FlushFlag flushFlag);
}
