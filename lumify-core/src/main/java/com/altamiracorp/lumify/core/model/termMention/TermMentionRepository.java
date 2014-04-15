package com.altamiracorp.lumify.core.model.termMention;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.ingest.term.extraction.TermMention;
import com.altamiracorp.lumify.core.model.audit.Audit;
import com.google.inject.Inject;
import com.google.inject.Singleton;

public abstract class TermMentionRepository extends Repository<TermMentionModel>{
    @Inject
    public TermMentionRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    public abstract TermMentionModel fromRow(Row row);

    public abstract Row toRow(TermMentionModel obj);

    public abstract String getTableName();

    public abstract Iterable<TermMentionModel> findByGraphVertexId(String graphVertexId, ModelUserContext modelUserContext);

    public abstract void updateColumnVisibility (TermMentionModel termMentionModel, String originalEdgeVisibility, String visibilityString, FlushFlag flushFlag);
}
