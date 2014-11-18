package io.lumify.core.model.workspace.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
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

    @Inject
    public WorkspaceDiffHelper(
            final Graph graph,
            final UserRepository userRepository) {
        this.graph = graph;
        this.userRepository = userRepository;
    }

    public ClientApiWorkspaceDiff diff(Workspace workspace, List<WorkspaceEntity> workspaceEntities, List<Edge> workspaceEdges, User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, WorkspaceRepository.VISIBILITY_STRING, workspace.getWorkspaceId());

        ClientApiWorkspaceDiff result = new ClientApiWorkspaceDiff();
        for (WorkspaceEntity workspaceEntity : workspaceEntities) {
            List<ClientApiWorkspaceDiff.Item> entityDiffs = diffWorkspaceEntity(workspace, workspaceEntity, authorizations);
            if (entityDiffs != null) {
                result.addAll(entityDiffs);
            }
        }

        for (Edge workspaceEdge : workspaceEdges) {
            List<ClientApiWorkspaceDiff.Item> entityDiffs = diffEdge(workspace, workspaceEdge, authorizations);
            if (entityDiffs != null) {
                result.addAll(entityDiffs);
            }
        }

        return result;
    }

    private List<ClientApiWorkspaceDiff.Item> diffEdge(Workspace workspace, Edge edge, Authorizations hiddenAuthorizations) {
        List<ClientApiWorkspaceDiff.Item> result = new ArrayList<ClientApiWorkspaceDiff.Item>();

        SandboxStatus sandboxStatus = GraphUtil.getSandboxStatus(edge, workspace.getWorkspaceId());
        boolean isPrivateChange = sandboxStatus != SandboxStatus.PUBLIC;
        boolean isPublicDelete = WorkspaceDiffHelper.isPublicDelete(edge, hiddenAuthorizations);
        if (isPrivateChange || isPublicDelete) {
            result.add(createWorkspaceDiffEdgeItem(edge, sandboxStatus, isPublicDelete));
        }

        diffProperties(workspace, edge, result, hiddenAuthorizations);

        return result;
    }

    public static boolean isPublicDelete(Edge edge, Authorizations authorizations) {
        return edge.isHidden(authorizations);
    }

    public static boolean isPublicDelete(Vertex vertex, Authorizations authorizations) {
        return vertex.isHidden(authorizations);
    }

    public static boolean isPublicDelete(Property property, Authorizations authorizations) {
        return property.isHidden(authorizations);
    }

    private ClientApiWorkspaceDiff.EdgeItem createWorkspaceDiffEdgeItem(Edge edge, SandboxStatus sandboxStatus, boolean deleted) {
        return new ClientApiWorkspaceDiff.EdgeItem(
                edge.getId(),
                edge.getLabel(),
                edge.getVertexId(Direction.OUT),
                edge.getVertexId(Direction.IN),
                JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(LumifyProperties.VISIBILITY_JSON.getProperty(edge))),
                sandboxStatus,
                deleted
        );
    }

    public List<ClientApiWorkspaceDiff.Item> diffWorkspaceEntity(Workspace workspace, WorkspaceEntity workspaceEntity, Authorizations authorizations) {
        List<ClientApiWorkspaceDiff.Item> result = new ArrayList<ClientApiWorkspaceDiff.Item>();

        Vertex entityVertex = this.graph.getVertex(workspaceEntity.getEntityVertexId(), FetchHint.ALL_INCLUDING_HIDDEN, authorizations);

        // vertex can be null if the user doesn't have access to the entity
        if (entityVertex == null) {
            return null;
        }

        SandboxStatus sandboxStatus = GraphUtil.getSandboxStatus(entityVertex, workspace.getWorkspaceId());
        boolean isPrivateChange = sandboxStatus != SandboxStatus.PUBLIC;
        boolean isPublicDelete = WorkspaceDiffHelper.isPublicDelete(entityVertex, authorizations);
        if (isPrivateChange || isPublicDelete) {
            result.add(createWorkspaceDiffVertexItem(entityVertex, sandboxStatus, workspaceEntity.isVisible(), isPublicDelete));
        }

        diffProperties(workspace, entityVertex, result, authorizations);

        return result;
    }

    private ClientApiWorkspaceDiff.VertexItem createWorkspaceDiffVertexItem(Vertex vertex, SandboxStatus sandboxStatus, boolean visible, boolean deleted) {
        String title = LumifyProperties.TITLE.getPropertyValue(vertex);
        return new ClientApiWorkspaceDiff.VertexItem(
                vertex.getId(),
                title,
                JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(LumifyProperties.VISIBILITY_JSON.getProperty(vertex))),
                sandboxStatus,
                deleted,
                visible
        );
    }

    private void diffProperties(Workspace workspace, Element element, List<ClientApiWorkspaceDiff.Item> result, Authorizations hiddenAuthorizations) {
        List<Property> properties = toList(element.getProperties());
        SandboxStatus[] propertyStatuses = GraphUtil.getPropertySandboxStatuses(properties, workspace.getWorkspaceId());
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            boolean isPrivateChange = propertyStatuses[i] != SandboxStatus.PUBLIC;
            boolean isPublicDelete = WorkspaceDiffHelper.isPublicDelete(property, hiddenAuthorizations);
            if (isPrivateChange || isPublicDelete) {
                Property existingProperty = null;
                if (isPrivateChange) {
                    existingProperty = findExistingProperty(properties, propertyStatuses, property);
                }
                result.add(createWorkspaceDiffPropertyItem(element, property, existingProperty, propertyStatuses[i], isPublicDelete));
            }
        }
    }

    private ClientApiWorkspaceDiff.PropertyItem createWorkspaceDiffPropertyItem(Element element, Property workspaceProperty, Property existingProperty, SandboxStatus sandboxStatus, boolean deleted) {
        JsonNode oldData = null;
        if (existingProperty != null) {
            oldData = JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(existingProperty));
        }
        JsonNode newData = JSONUtil.toJsonNode(JsonSerializer.toJsonProperty(workspaceProperty));
        return new ClientApiWorkspaceDiff.PropertyItem(
                element.getId(),
                workspaceProperty.getName(),
                workspaceProperty.getKey(),
                oldData,
                newData,
                sandboxStatus,
                deleted,
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
