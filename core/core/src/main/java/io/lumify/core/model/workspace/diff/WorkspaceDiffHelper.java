package io.lumify.core.model.workspace.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.lumify.core.formula.FormulaEvaluator;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceEntity;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.GraphUtil;
import io.lumify.core.util.JSONUtil;
import io.lumify.core.util.JsonSerializer;
import io.lumify.web.clientapi.model.ClientApiWorkspaceDiff;
import io.lumify.web.clientapi.model.SandboxStatus;
import org.securegraph.*;

import java.util.ArrayList;
import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

public class WorkspaceDiffHelper {
    private final Graph graph;
    private final UserRepository userRepository;
    private final FormulaEvaluator formulaEvaluator;

    @Inject
    public WorkspaceDiffHelper(
            final Graph graph,
            final UserRepository userRepository,
            final FormulaEvaluator formulaEvaluator
    ) {
        this.graph = graph;
        this.userRepository = userRepository;
        this.formulaEvaluator = formulaEvaluator;
    }

    public ClientApiWorkspaceDiff diff(Workspace workspace, List<WorkspaceEntity> workspaceEntities, List<Edge> workspaceEdges, FormulaEvaluator.UserContext userContext, User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, WorkspaceRepository.VISIBILITY_STRING, workspace.getWorkspaceId());

        ClientApiWorkspaceDiff result = new ClientApiWorkspaceDiff();
        for (WorkspaceEntity workspaceEntity : workspaceEntities) {
            List<ClientApiWorkspaceDiff.Item> entityDiffs = diffWorkspaceEntity(workspace, workspaceEntity, userContext, authorizations);
            if (entityDiffs != null) {
                result.addAll(entityDiffs);
            }
        }

        for (Edge workspaceEdge : workspaceEdges) {
            List<ClientApiWorkspaceDiff.Item> entityDiffs = diffEdge(workspace, workspaceEdge);
            if (entityDiffs != null) {
                result.addAll(entityDiffs);
            }
        }

        return result;
    }

    private List<ClientApiWorkspaceDiff.Item> diffEdge(Workspace workspace, Edge edge) {
        List<ClientApiWorkspaceDiff.Item> result = new ArrayList<>();

        SandboxStatus sandboxStatus = GraphUtil.getSandboxStatus(edge, workspace.getWorkspaceId());
        if (sandboxStatus != SandboxStatus.PUBLIC) {
            result.add(createWorkspaceDiffEdgeItem(edge, sandboxStatus));
        }

        diffProperties(workspace, edge, result);

        return result;
    }

    private ClientApiWorkspaceDiff.EdgeItem createWorkspaceDiffEdgeItem(Edge edge, SandboxStatus sandboxStatus) {
        Property visibilityJsonProperty = LumifyProperties.VISIBILITY_JSON.getProperty(edge);
        JsonNode visibilityJson = visibilityJsonProperty == null ? null : JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(visibilityJsonProperty));
        return new ClientApiWorkspaceDiff.EdgeItem(
                edge.getId(),
                edge.getLabel(),
                edge.getVertexId(Direction.OUT),
                edge.getVertexId(Direction.IN),
                visibilityJson,
                sandboxStatus
        );
    }

    public List<ClientApiWorkspaceDiff.Item> diffWorkspaceEntity(Workspace workspace, WorkspaceEntity workspaceEntity, FormulaEvaluator.UserContext userContext, Authorizations authorizations) {
        List<ClientApiWorkspaceDiff.Item> result = new ArrayList<>();

        Vertex entityVertex = this.graph.getVertex(workspaceEntity.getEntityVertexId(), authorizations);

        // vertex can be null if the user doesn't have access to the entity
        if (entityVertex == null) {
            return null;
        }

        SandboxStatus sandboxStatus = GraphUtil.getSandboxStatus(entityVertex, workspace.getWorkspaceId());
        if (sandboxStatus != SandboxStatus.PUBLIC) {
            result.add(createWorkspaceDiffVertexItem(entityVertex, sandboxStatus, userContext, workspaceEntity.isVisible()));
        }

        diffProperties(workspace, entityVertex, result);

        return result;
    }

    private ClientApiWorkspaceDiff.VertexItem createWorkspaceDiffVertexItem(Vertex vertex, SandboxStatus sandboxStatus, FormulaEvaluator.UserContext userContext, boolean visible) {
        String vertexId = vertex.getId();
        String title = formulaEvaluator.evaluateTitleFormula(vertex, userContext, null);
        String conceptType = LumifyProperties.CONCEPT_TYPE.getPropertyValue(vertex);
        Property visibilityJsonProperty = LumifyProperties.VISIBILITY_JSON.getProperty(vertex);
        JsonNode visibilityJson = visibilityJsonProperty == null ? null : JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(visibilityJsonProperty));
        return new ClientApiWorkspaceDiff.VertexItem(
                vertexId,
                title,
                conceptType,
                visibilityJson,
                sandboxStatus,
                visible
        );
    }

    private void diffProperties(Workspace workspace, Element element, List<ClientApiWorkspaceDiff.Item> result) {
        List<Property> properties = toList(element.getProperties());
        SandboxStatus[] propertyStatuses = GraphUtil.getPropertySandboxStatuses(properties, workspace.getWorkspaceId());
        for (int i = 0; i < properties.size(); i++) {
            if (propertyStatuses[i] != SandboxStatus.PUBLIC) {
                Property property = properties.get(i);
                Property existingProperty = findExistingProperty(properties, propertyStatuses, property);
                result.add(createWorkspaceDiffPropertyItem(element, property, existingProperty, propertyStatuses[i]));
            }
        }
    }

    private ClientApiWorkspaceDiff.PropertyItem createWorkspaceDiffPropertyItem(Element element, Property workspaceProperty, Property existingProperty, SandboxStatus sandboxStatus) {
        JsonNode oldData = null;
        if (existingProperty != null) {
            oldData = JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(existingProperty));
        }
        JsonNode newData = JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(workspaceProperty));
        return new ClientApiWorkspaceDiff.PropertyItem(
                element instanceof Edge ? "edge" : "vertex",
                element.getId(),
                workspaceProperty.getName(),
                workspaceProperty.getKey(),
                oldData,
                newData,
                sandboxStatus,
                workspaceProperty.getVisibility().getVisibilityString()
        );
    }

    private Property findExistingProperty(List<Property> properties, SandboxStatus[] propertyStatuses, Property workspaceProperty) {
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            if (property.getName().equals(workspaceProperty.getName())
                    && property.getKey().equals(workspaceProperty.getKey())
                    && propertyStatuses[i] == SandboxStatus.PUBLIC) {
                return property;
            }
        }
        return null;
    }
}
