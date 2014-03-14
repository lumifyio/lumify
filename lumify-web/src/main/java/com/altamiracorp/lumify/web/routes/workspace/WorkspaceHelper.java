package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.ingest.term.extraction.TermMention;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.lumify.core.model.textHighlighting.TermMentionOffsetItem;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONObject;

import java.util.Iterator;

@Singleton
public class WorkspaceHelper {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WorkspaceHelper.class);
    private final ModelSession modelSession;
    private final TermMentionRepository termMentionRepository;
    private final AuditRepository auditRepository;
    private final OntologyRepository ontologyRepository;
    private final Graph graph;

    @Inject
    public WorkspaceHelper (final ModelSession modelSession,
                            final TermMentionRepository termMentionRepository,
                            final AuditRepository auditRepository,
                            final OntologyRepository ontologyRepository,
                            final Graph graph) {
        this.modelSession = modelSession;
        this.termMentionRepository = termMentionRepository;
        this.auditRepository = auditRepository;
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
    }

    public JSONObject unresolveTerm(Vertex vertex, TermMentionModel termMention, LumifyVisibility visibility, ModelUserContext modelUserContext, User user, Authorizations authorizations) {
        JSONObject result = new JSONObject();
        if (termMention == null) {
            LOGGER.warn("invalid term mention row");
            result.put("failure", true);
        } else {
            Vertex artifactVertex = graph.getVertex(termMention.getRowKey().getGraphVertexId(), authorizations);
            // Clean up term mentions if system analytics wasn't performed on term
            String columnFamilyName = termMention.getMetadata().getColumnFamilyName();
            String columnName = termMention.getMetadata().VERTEX_ID;
            String analyticProcess = termMention.getMetadata().getAnalyticProcess();

            if (analyticProcess == null) {
                modelSession.deleteRow(termMention.getTableName(), termMention.getRowKey());
            } else {
                termMention.get(columnFamilyName).getColumn(columnName).setDirty(true);
                modelSession.deleteColumn(termMention, termMention.getTableName(), columnFamilyName, columnName);
                termMention.getMetadata().setVertexId("", visibility.getVisibility());

                TermMentionOffsetItem offsetItem = new TermMentionOffsetItem(termMention);
                result = offsetItem.toJson();
            }

            // If there is only instance of the term entity in this artifact delete the relationship
            Iterator<TermMentionModel> termMentionModels = termMentionRepository.findByGraphVertexId(termMention.getRowKey().getGraphVertexId(), modelUserContext).iterator();
            boolean deleteEdge = false;
            Object edgeId = null;
            int termCount = 0;
            while (termMentionModels.hasNext()) {
                TermMentionModel termMentionModel = termMentionModels.next();
                Object termMentionId = termMentionModel.getMetadata().getGraphVertexId();
                if (termMentionId != null && termMentionId.equals(vertex.getId())) {
                    termCount++;
                    break;
                }
            }
            if (termCount == 1) {
                Iterable<Edge> edges = artifactVertex.getEdges(vertex, Direction.OUT, LabelName.RAW_HAS_ENTITY.toString(), authorizations);
                Edge edge = edges.iterator().next();
                if (edge != null) {
                    String label = ontologyRepository.getDisplayNameForLabel(edge.getLabel());
                    edgeId = edge.getId();
                    graph.removeEdge(edge, authorizations);
                    deleteEdge = true;
                    graph.flush();
                    auditRepository.auditRelationship(AuditAction.DELETE, artifactVertex, vertex, label, "", "", user, visibility.getVisibility());
                }
            }

            if (deleteEdge) {
                result.put("deleteEdge", deleteEdge);
                result.put("edgeId", edgeId);
            }
        }
        return result;
    }
}
