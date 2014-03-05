package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.exception.LumifyAccessDeniedException;
import com.altamiracorp.lumify.core.exception.LumifyResourceNotFoundException;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Relationship;
import com.altamiracorp.lumify.core.model.user.AuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.diff.DiffItem;
import com.altamiracorp.lumify.core.model.workspace.diff.WorkspaceDiff;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class WorkspaceRepository {
    public static final String VISIBILITY_STRING = "workspace";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    public static final String WORKSPACE_CONCEPT_NAME = "http://lumify.io/workspace";
    public static final String WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME = "http://lumify.io/workspace/toEntity";
    public static final String WORKSPACE_TO_USER_RELATIONSHIP_NAME = "http://lumify.io/workspace/toUser";
    public static final String WORKSPACE_ID_PREFIX = "WORKSPACE_";
    private final Graph graph;
    private final String workspaceConceptId;
    private final String workspaceToEntityRelationshipId;
    private final String workspaceToUserRelationshipId;
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;
    private final WorkspaceDiff workspaceDiff;

    @Inject
    public WorkspaceRepository(
            final Graph graph,
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final AuthorizationRepository authorizationRepository,
            final WorkspaceDiff workspaceDiff) {
        this.graph = graph;
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
        this.workspaceDiff = workspaceDiff;

        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        authorizationRepository.addAuthorizationToGraph(LumifyVisibility.VISIBILITY_STRING);

        Concept entityConcept = ontologyRepository.getConceptByName(OntologyRepository.TYPE_ENTITY);

        Concept workspaceConcept = ontologyRepository.getOrCreateConcept(null, WORKSPACE_CONCEPT_NAME, "workspace");
        workspaceConceptId = workspaceConcept.getId();

        Relationship workspaceToEntityRelationship = ontologyRepository.getOrCreateRelationshipType(workspaceConcept, entityConcept, WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME, "workspace to entity");
        workspaceToEntityRelationshipId = workspaceToEntityRelationship.getId();

        Relationship workspaceToUserRelationship = ontologyRepository.getOrCreateRelationshipType(workspaceConcept, entityConcept, WORKSPACE_TO_USER_RELATIONSHIP_NAME, "workspace to user");
        workspaceToUserRelationshipId = workspaceToUserRelationship.getId();
    }

    public void delete(Workspace workspace, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

        Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, VISIBILITY.getVisibility().getVisibilityString(), workspace.getId());
        graph.removeVertex(workspace.getVertex(), authorizations);
        graph.flush();

        authorizationRepository.removeAuthorizationFromGraph(workspace.getId());
    }

    public Workspace findById(String workspaceId, User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspaceId);
        Vertex workspaceVertex = graph.getVertex(workspaceId, authorizations);
        if (workspaceVertex == null) {
            return null;
        }
        Workspace workspace = new Workspace(workspaceVertex, this, user);
        if (!doesUserHaveReadAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have read access to workspace " + workspace.getId(), user, workspace.getId());
        }
        return workspace;
    }

    public Workspace add(String title, User user) {
        Vertex userVertex = this.userRepository.findById(user.getUserId());
        checkNotNull(userVertex, "Could not find user: " + user.getUserId());

        String workspaceId = WORKSPACE_ID_PREFIX + graph.getIdGenerator().nextId();
        authorizationRepository.addAuthorizationToGraph(workspaceId);

        Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, VISIBILITY_STRING, workspaceId);
        VertexBuilder workspaceVertexBuilder = graph.prepareVertex(workspaceId, VISIBILITY.getVisibility(), authorizations);
        OntologyLumifyProperties.CONCEPT_TYPE.setProperty(workspaceVertexBuilder, workspaceConceptId, VISIBILITY.getVisibility());
        WorkspaceLumifyProperties.TITLE.setProperty(workspaceVertexBuilder, title, VISIBILITY.getVisibility());
        Vertex workspaceVertex = workspaceVertexBuilder.save();

        EdgeBuilder edgeBuilder = graph.prepareEdge(workspaceVertex, userVertex, workspaceToUserRelationshipId, VISIBILITY.getVisibility(), authorizations);
        WorkspaceLumifyProperties.WORKSPACE_TO_USER_IS_CREATOR.setProperty(edgeBuilder, true, VISIBILITY.getVisibility());
        WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.setProperty(edgeBuilder, WorkspaceAccess.WRITE.toString(), VISIBILITY.getVisibility());
        edgeBuilder.save();

        graph.flush();
        return new Workspace(workspaceVertex, this, user);
    }

    public Iterable<Workspace> findAll(User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
        Iterable<Vertex> vertices = graph.getVertex(user.getUserId(), authorizations).getVertices(Direction.IN, workspaceToUserRelationshipId, authorizations);
        return Workspace.toWorkspaceIterable(vertices, this, user);
    }

    public void setTitle(Workspace workspace, String title, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }
        WorkspaceLumifyProperties.TITLE.setProperty(workspace.getVertex(), title, VISIBILITY.getVisibility());
        graph.flush();
    }

    public List<WorkspaceUser> findUsersWithAccess(final Workspace workspace, final User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());
        Iterable<Edge> userEdges = workspace.getVertex().query(authorizations).edges(workspaceToUserRelationshipId);
        return toList(new ConvertingIterable<Edge, WorkspaceUser>(userEdges) {
            @Override
            protected WorkspaceUser convert(Edge edge) {
                String userId = edge.getOtherVertexId(workspace.getVertex().getId()).toString();

                String accessString = WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.getPropertyValue(edge);
                WorkspaceAccess workspaceAccess = WorkspaceAccess.NONE;
                if (accessString != null && accessString.length() > 0) {
                    workspaceAccess = WorkspaceAccess.valueOf(accessString);
                }

                boolean isCreator = WorkspaceLumifyProperties.WORKSPACE_TO_USER_IS_CREATOR.getPropertyValue(edge, false);

                return new WorkspaceUser(userId, workspaceAccess, isCreator);
            }
        });
    }

    public List<WorkspaceEntity> findEntities(final Workspace workspace, User user) {
        if (!doesUserHaveReadAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have read access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());
        Iterable<Edge> entityEdges = workspace.getVertex().query(authorizations).edges(workspaceToEntityRelationshipId);
        return toList(new ConvertingIterable<Edge, WorkspaceEntity>(entityEdges) {
            @Override
            protected WorkspaceEntity convert(Edge edge) {
                Object entityVertexId = edge.getOtherVertexId(workspace.getVertex().getId());

                int graphPositionX = WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_X.getPropertyValue(edge, 0);
                int graphPositionY = WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y.getPropertyValue(edge, 0);

                return new WorkspaceEntity(entityVertexId, graphPositionX, graphPositionY);
            }
        });
    }

    public Workspace copy(Workspace workspace, User user) {
        Workspace newWorkspace = add("Copy of " + workspace.getTitle(), user);

        List<WorkspaceEntity> entities = findEntities(workspace, user);
        for (WorkspaceEntity entity : entities) {
            updateEntityOnWorkspace(newWorkspace, entity.getEntityVertexId(), entity.getGraphPositionX(), entity.getGraphPositionY(), user);
        }

        // TODO should we copy users?

        graph.flush();

        return newWorkspace;
    }

    public void deleteEntityFromWorkspace(Workspace workspace, Object vertexId, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());
        Vertex otherVertex = graph.getVertex(vertexId, authorizations);
        if (otherVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find vertex: " + vertexId, vertexId);
        }
        List<Edge> edges = toList(workspace.getVertex().getEdges(otherVertex, Direction.BOTH, authorizations));
        for (Edge edge : edges) {
            graph.removeEdge(edge, authorizations);
        }
        graph.flush();
    }

    public void updateEntityOnWorkspace(Workspace workspace, Object vertexId, int graphPositionX, int graphPositionY, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());

        Vertex workspaceVertex = graph.getVertex(workspace.getId(), authorizations);
        if (workspaceVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find workspace vertex: " + workspace.getId(), workspace.getId());
        }

        Vertex otherVertex = graph.getVertex(vertexId, authorizations);
        if (otherVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find vertex: " + vertexId, vertexId);
        }

        List<Edge> existingEdges = toList(workspaceVertex.getEdges(otherVertex, Direction.BOTH, authorizations));
        if (existingEdges.size() > 0) {
            for (Edge existingEdge : existingEdges) {
                ElementMutation<Edge> m = existingEdge.prepareMutation();
                WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_X.setProperty(m, graphPositionX, VISIBILITY.getVisibility());
                WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y.setProperty(m, graphPositionY, VISIBILITY.getVisibility());
                m.save();
            }
        } else {
            EdgeBuilder edgeBuilder = graph.prepareEdge(workspace.getVertex(), otherVertex, workspaceToEntityRelationshipId, VISIBILITY.getVisibility(), authorizations);
            WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_X.setProperty(edgeBuilder, graphPositionX, VISIBILITY.getVisibility());
            WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y.setProperty(edgeBuilder, graphPositionY, VISIBILITY.getVisibility());
            edgeBuilder.save();
        }
        graph.flush();
    }

    public void deleteUserFromWorkspace(Workspace workspace, String userId, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, VISIBILITY_STRING, workspace.getId());
        Vertex userVertex = userRepository.findById(userId);
        if (userVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find user: " + userId, userId);
        }
        List<Edge> edges = toList(workspace.getVertex().getEdges(userVertex, Direction.BOTH, workspaceToUserRelationshipId, authorizations));
        for (Edge edge : edges) {
            graph.removeEdge(edge, authorizations);
        }
        graph.flush();
    }

    public boolean doesUserHaveWriteAccess(Workspace workspace, User user) {
        List<WorkspaceUser> usersWithAccess = findUsersWithAccess(workspace, user);
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId()) && userWithAccess.getWorkspaceAccess() == WorkspaceAccess.WRITE) {
                return true;
            }
        }
        return false;
    }

    public boolean doesUserHaveReadAccess(Workspace workspace, User user) {
        List<WorkspaceUser> usersWithAccess = findUsersWithAccess(workspace, user);
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId())
                    && (userWithAccess.getWorkspaceAccess() == WorkspaceAccess.WRITE || userWithAccess.getWorkspaceAccess() == WorkspaceAccess.READ)) {
                return true;
            }
        }
        return false;
    }

    public void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());
        Vertex userVertex = userRepository.findById(userId);
        if (userVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find user: " + userId, userId);
        }

        Vertex workspaceVertex = graph.getVertex(workspace.getId(), authorizations);
        if (workspaceVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find workspace vertex: " + workspace.getId(), workspace.getId());
        }

        List<Edge> existingEdges = toList(workspaceVertex.getEdges(userVertex, Direction.OUT, workspaceToUserRelationshipId, authorizations));
        if (existingEdges.size() > 0) {
            for (Edge existingEdge : existingEdges) {
                WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.setProperty(existingEdge, workspaceAccess.toString(), VISIBILITY.getVisibility());
            }
        } else {
            EdgeBuilder edgeBuilder = graph.prepareEdge(workspace.getVertex(), userVertex, workspaceToUserRelationshipId, VISIBILITY.getVisibility(), authorizations);
            WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.setProperty(edgeBuilder, workspaceAccess.toString(), VISIBILITY.getVisibility());
            edgeBuilder.save();
        }

        graph.flush();
    }

    public List<DiffItem> getDiff(Workspace workspace, User user) {
        if (!doesUserHaveReadAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

        List<WorkspaceEntity> workspaceEntities = findEntities(workspace, user);
        return workspaceDiff.diff(workspace, workspaceEntities, user);
    }
}
