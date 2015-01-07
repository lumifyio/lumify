package io.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.Row;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.user.User;
import org.securegraph.*;
import org.securegraph.mutation.ElementMutation;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAuditRepository extends AuditRepository {
    private List<VertexElementMutation> vertexElementMutations = new ArrayList<VertexElementMutation>();
    private List<AnalyzedBy> analyzedBys = new ArrayList<AnalyzedBy>();

    public InMemoryAuditRepository() {
        super(null);
    }

    @Override
    public Audit fromRow(Row row) {
        throw new LumifyException("not implemented");
    }

    @Override
    public Row toRow(Audit audit) {
        throw new LumifyException("not implemented");
    }

    @Override
    public String getTableName() {
        throw new LumifyException("not implemented");
    }

    @Override
    public Iterable<Audit> getAudits(String vertexId, String workspaceId, Authorizations authorizations) {
        throw new LumifyException("not implemented");
    }

    @Override
    public Audit auditVertex(AuditAction auditAction, Object vertexId, String process, String comment, User user, Visibility visibility) {
        throw new LumifyException("not implemented");
    }

    @Override
    public Audit auditEntityProperty(AuditAction action, Object id, String propertyKey, String propertyName, Object oldValue, Object newValue, String process, String comment, Metadata metadata, User user, Visibility visibility) {
        throw new LumifyException("not implemented");
    }

    @Override
    public List<Audit> auditRelationship(AuditAction action, Vertex sourceVertex, Vertex destVertex, Edge edge, String process, String comment, User user, Visibility visibility) {
        throw new LumifyException("not implemented");
    }

    @Override
    public List<Audit> auditRelationshipProperty(AuditAction action, String sourceId, String destId, String propertyKey, String propertyName, Object oldValue, Object newValue, Edge edge, String process, String comment, User user, Visibility visibility) {
        throw new LumifyException("not implemented");
    }

    @Override
    public Audit auditAnalyzedBy(AuditAction action, Vertex vertex, String process, User user, Visibility visibility) {
        analyzedBys.add(new AnalyzedBy(action, vertex, process, user, visibility));
        return new Audit("inmemory");
    }

    @Override
    public void auditVertexElementMutation(AuditAction action, ElementMutation<Vertex> vertexElementMutation, Vertex vertex, String process, User user, Visibility visibility) {
        vertexElementMutations.add(new VertexElementMutation(action, vertexElementMutation, vertex, process, user, visibility));
    }

    @Override
    public void auditEdgeElementMutation(AuditAction action, ElementMutation<Edge> edgeElementMutation, Edge edge, Vertex sourceVertex, Vertex destVertex, String process, User user, Visibility visibility) {
        throw new LumifyException("not implemented");
    }

    @Override
    public void updateColumnVisibility(Audit audit, Visibility originalEdgeVisibility, String visibilityString) {
        throw new LumifyException("not implemented");
    }

    public List<VertexElementMutation> getVertexElementMutations() {
        return vertexElementMutations;
    }

    public List<AnalyzedBy> getAnalyzedBys() {
        return analyzedBys;
    }

    public static class AnalyzedBy {
        private final AuditAction action;
        private final Vertex vertex;
        private final String process;
        private final User user;
        private final Visibility visibility;

        public AnalyzedBy(AuditAction action, Vertex vertex, String process, User user, Visibility visibility) {
            this.action = action;
            this.vertex = vertex;
            this.process = process;
            this.user = user;
            this.visibility = visibility;
        }
    }

    public static class VertexElementMutation {
        private final AuditAction action;
        private final ElementMutation<Vertex> vertexElementMutation;
        private final Vertex vertex;
        private final String process;
        private final User user;
        private final Visibility visibility;

        public VertexElementMutation(AuditAction action, ElementMutation<Vertex> vertexElementMutation, Vertex vertex, String process, User user, Visibility visibility) {
            this.action = action;
            this.vertexElementMutation = vertexElementMutation;
            this.vertex = vertex;
            this.process = process;
            this.user = user;
            this.visibility = visibility;
        }

        public AuditAction getAction() {
            return action;
        }

        public ElementMutation<Vertex> getVertexElementMutation() {
            return vertexElementMutation;
        }

        public Vertex getVertex() {
            return vertex;
        }

        public String getProcess() {
            return process;
        }

        public User getUser() {
            return user;
        }

        public Visibility getVisibility() {
            return visibility;
        }
    }
}
