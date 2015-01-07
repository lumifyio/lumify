package io.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Row;
import com.beust.jcommander.internal.Nullable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.user.User;
import org.securegraph.*;
import org.securegraph.mutation.ElementMutation;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class NoOpAuditRepository extends AuditRepository {
    @Inject
    public NoOpAuditRepository(@Nullable final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public Audit fromRow(Row row) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Row toRow(Audit audit) {
        throw new RuntimeException("not supported");
    }

    @Override
    public String getTableName() {
        throw new RuntimeException("not supported");
    }

    @Override
    public Iterable<Audit> getAudits(String vertexId, String workspaceId, Authorizations authorizations) {
        return new ArrayList<Audit>();
    }

    @Override
    public Audit auditVertex(AuditAction auditAction, Object vertexId, String process, String comment, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Audit auditEntityProperty(AuditAction action, Object id, String propertyKey, String propertyName, Object oldValue,
                                     Object newValue, String process, String comment, Metadata metadata, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public List<Audit> auditRelationship(AuditAction action, Vertex sourceVertex, Vertex destVertex, Edge edge, String process, String comment, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public List<Audit> auditRelationshipProperty(AuditAction action, String sourceId, String destId, String propertyKey,
                                                 String propertyName, Object oldValue, Object newValue, Edge edge, String process, String comment, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public Audit auditAnalyzedBy(AuditAction action, Vertex vertex, String process, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void auditVertexElementMutation(AuditAction action, ElementMutation<Vertex> vertexElementMutation, Vertex vertex, String process, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void auditEdgeElementMutation(AuditAction action, ElementMutation<Edge> edgeElementMutation, Edge edge, Vertex sourceVertex, Vertex destVertex, String process, User user, Visibility visibility) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void updateColumnVisibility(Audit audit, Visibility originalEdgeVisibility, String visibilityString) {
        throw new RuntimeException("not supported");
    }
}
