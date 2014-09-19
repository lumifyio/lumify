package io.lumify.core.model.audit;

import com.google.inject.Inject;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.version.VersionService;
import org.json.JSONObject;
import org.securegraph.*;

import java.util.Date;
import java.util.Iterator;

public class AuditBuilder {
    private static final String AUDIT_VERTEX_ID_PREFIX = "AUDIT_";
    private VersionService versionService;
    private Graph graph;
    private VisibilityTranslator visibilityTranslator;
    private User user;
    private String analyzedBy;
    private Long unixBuildTime;
    private String version;
    private String scmBuildNumber;
    private AuditAction auditAction;
    private AuditType auditType;
    private Date date;

    public AuditBuilder() {
        this.scmBuildNumber = versionService.getScmBuildNumber() != null ? versionService.getScmBuildNumber() : "";
        this.unixBuildTime = versionService.getUnixBuildTime() != null ? versionService.getUnixBuildTime() : -1L;
        this.version = versionService.getVersion() != null ? versionService.getVersion() : "";
        this.date = new Date();
    }

    public AuditBuilder user(User user) {
        this.user = user;
        return this;
    }

    public AuditBuilder analyzedBy(String analyzedBy) {
        this.analyzedBy = analyzedBy;
        return this;
    }

    public AuditBuilder auditAction(AuditAction auditAction) {
        this.auditAction = auditAction;
        return this;
    }

    public AuditBuilder auditType (AuditType auditType) {
        this.auditType = auditType;
        return this;
    }

    public void auditVertex(Vertex vertexToAudit, Authorizations authorizations) {
        JSONObject visibilitySource = LumifyProperties.VISIBILITY_SOURCE.getPropertyValue(vertexToAudit);
        Visibility auditVisibility = LumifyVisibility.and(visibilityTranslator.toVisibility(visibilitySource).getVisibility(), AuditRepository.AUDIT_VISIBILITY);
        VertexBuilder auditVertexBuilder = setupLumifyAuditVertex(auditVisibility);
        LumifyProperties.AUDITED_VERTEX_ID.setProperty(auditVertexBuilder, vertexToAudit.getId(), auditVisibility);
        LumifyProperties.VISIBILITY_SOURCE.setProperty(auditVertexBuilder, visibilitySource, auditVisibility);
        Vertex auditVertex = auditVertexBuilder.save(authorizations);
        createLumifyAuditRelationships(auditVertex, user, authorizations);

        // Creating audit vertices for all of the properties
        Iterator properties = vertexToAudit.getProperties().iterator();
        while (properties.hasNext()) {
            auditProperty (vertexToAudit, null, (Property)properties.next(), authorizations);
        }

        graph.flush();
    }

    public void auditRelationship (Edge edge, Authorizations authorizations) {
        JSONObject edgeVisibilitySource = LumifyProperties.VISIBILITY_SOURCE.getPropertyValue(edge);
        Visibility auditVisibility = LumifyVisibility.and(visibilityTranslator.toVisibility(edgeVisibilitySource).getVisibility(), AuditRepository.AUDIT_VISIBILITY);
        Vertex sourceVertex = edge.getVertex(Direction.OUT, authorizations);

    }

    private void auditRelationship (String sourceVertexId, String destVertexId, JSONObject visibilitySource, Visibility auditVisibility) {
        VertexBuilder auditSourceVertexBuilder = setupLumifyAuditVertex(vertexId, visibilitySource, auditVisibility);
        LumifyProperties.VISIBILITY_SOURCE.setProperty(auditSourceVertexBuilder, visibilitySource, auditVisibility);
        LumifyProperties.AUDIT_RELATIONSHIP_SOURCE_VERTEX_ID.setProperty(auditSourceVertexBuilder, vertexId, auditVisibility);
        LumifyProperties.AUDIT_RELATIONSHIP_DEST_VERTEX_ID.setProperty(auditSourceVertexBuilder, vertexId, auditVisibility);
    }

    private void auditProperty (Vertex vertexToAudit, Object oldPropertyValue, Property newProperty, Authorizations authorizations) {
        JSONObject visibilitySource = LumifyProperties.VISIBILITY_SOURCE.getPropertyValue(vertexToAudit);
        Visibility auditVisibility = LumifyVisibility.and(visibilityTranslator.toVisibility(visibilitySource).getVisibility(), AuditRepository.AUDIT_VISIBILITY);
        VertexBuilder auditVertexBuilder = setupLumifyAuditVertex(auditVisibility);
        LumifyProperties.AUDITED_VERTEX_ID.setProperty(auditVertexBuilder, vertexToAudit.getId(), auditVisibility);
        LumifyProperties.VISIBILITY_SOURCE.setProperty(auditVertexBuilder, visibilitySource, auditVisibility);
        if (oldPropertyValue != null) {
            LumifyProperties.AUDIT_PROPERTY_OLD_VALUE.setProperty(auditVertexBuilder, oldPropertyValue.toString(), auditVisibility);
        }
        LumifyProperties.AUDIT_PROPERTY_KEY.setProperty(auditVertexBuilder, newProperty.getKey(), auditVisibility);
        LumifyProperties.AUDIT_PROPERTY_NAME.setProperty(auditVertexBuilder, newProperty.getName(), auditVisibility);
        LumifyProperties.AUDIT_PROPERTY_NEW_VALUE.setProperty(auditVertexBuilder, newProperty.getValue().toString(), auditVisibility);
        LumifyProperties.AUDIT_PROPERTY_METADATA.setProperty(auditVertexBuilder, newProperty.getMetadata().toString(), auditVisibility);
        Vertex auditVertex = auditVertexBuilder.save(authorizations);
        createLumifyAuditRelationships(auditVertex, user, authorizations);
    }

    private VertexBuilder setupLumifyAuditVertex (Visibility auditVisibility) {
        String auditVertexId = createVertexId();
        VertexBuilder auditVertexBuilder = graph.prepareVertex(auditVertexId, auditVisibility);
        LumifyProperties.AUDIT_SOURCE_VERSION.setProperty(auditVertexBuilder, this.version, auditVisibility);
        LumifyProperties.AUDIT_SCM_BUILD_TIME.setProperty(auditVertexBuilder, this.scmBuildNumber, auditVisibility);
        LumifyProperties.AUDIT_UNIX_BUILD_TIME.setProperty(auditVertexBuilder, this.unixBuildTime, auditVisibility);
        LumifyProperties.AUDIT_DATE_TIME.setProperty(auditVertexBuilder, this.date, auditVisibility);
        LumifyProperties.AUDIT_ACTION.setProperty(auditVertexBuilder, this.auditAction.toString(), auditVisibility);
        LumifyProperties.AUDIT_ANALYZED_BY.setProperty(auditVertexBuilder, this.analyzedBy, auditVisibility);
        return auditVertexBuilder;
    }

    private void createLumifyAuditRelationships (Vertex vertexToAudit, User user, Authorizations authorizations) {
        JSONObject visibilitySource = LumifyProperties.VISIBILITY_SOURCE.getPropertyValue(vertexToAudit);
        Visibility auditVisibility = LumifyVisibility.and(visibilityTranslator.toVisibility(visibilitySource).getVisibility(), AuditRepository.AUDIT_VISIBILITY);
        String auditVertexId = vertexToAudit.getId();
        String hasAuditId = createHasAuditId(auditVertexId);
        graph.addEdge(hasAuditId, vertexToAudit, vertexToAudit, LumifyProperties.AUDIT_HAS_AUDIT_LABEL, auditVisibility, authorizations);
        String hasActorId = createHasActorId(auditVertexId);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        if (userVertex != null) {
            graph.addEdge(hasActorId, vertexToAudit, userVertex, LumifyProperties.AUDIT_HAS_ACTOR_LABEL, auditVisibility, authorizations);
        }
    }

    private String createVertexId() {
        return AUDIT_VERTEX_ID_PREFIX
                + "-"
                + graph.getIdGenerator().nextId();
    }

    private String createHasAuditId(String auditVertexId) {
        return auditVertexId + "_hasAudit";
    }

    private String createHasActorId(String auditVertexId) {
        return auditVertexId + "_hasActor";
    }

    @Inject
    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public void setVisibilityTranslator (VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }
}
