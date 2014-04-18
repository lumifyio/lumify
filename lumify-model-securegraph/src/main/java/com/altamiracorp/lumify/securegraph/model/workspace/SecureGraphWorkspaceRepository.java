package com.altamiracorp.lumify.securegraph.model.workspace;

import com.altamiracorp.lumify.core.exception.LumifyAccessDeniedException;
import com.altamiracorp.lumify.core.exception.LumifyResourceNotFoundException;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Relationship;
import com.altamiracorp.lumify.core.model.user.AuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.*;
import com.altamiracorp.lumify.core.model.workspace.diff.DiffItem;
import com.altamiracorp.lumify.core.model.workspace.diff.WorkspaceDiff;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import com.altamiracorp.securegraph.util.VerticesToEdgeIdsIterable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.altamiracorp.securegraph.util.IterableUtils.toList;
import static com.altamiracorp.securegraph.util.IterableUtils.toSet;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class SecureGraphWorkspaceRepository extends WorkspaceRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SecureGraphWorkspaceRepository.class);
    private Graph graph;
    private String workspaceConceptId;
    private String workspaceToEntityRelationshipId;
    private String workspaceToUserRelationshipId;
    private UserRepository userRepository;
    private AuthorizationRepository authorizationRepository;
    private WorkspaceDiff workspaceDiff;
    private Cache<String, Boolean> usersWithReadAccessCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();
    private Cache<String, Boolean> usersWithWriteAccessCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    @Inject
    public SecureGraphWorkspaceRepository(
            OntologyRepository ontologyRepository,
            Graph graph,
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository,
            WorkspaceDiff workspaceDiff) {
        this.graph = graph;
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
        this.workspaceDiff = workspaceDiff;

        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        authorizationRepository.addAuthorizationToGraph(LumifyVisibility.SUPER_USER_VISIBILITY_STRING);

        Concept rootConcept = ontologyRepository.getConceptByIRI(OntologyRepository.ROOT_CONCEPT_IRI);

        Concept workspaceConcept = ontologyRepository.getOrCreateConcept(null, WORKSPACE_CONCEPT_NAME, "workspace");
        workspaceConceptId = workspaceConcept.getTitle();

        Relationship workspaceToEntityRelationship = ontologyRepository.getOrCreateRelationshipType(workspaceConcept, rootConcept, WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME, "workspace to entity");
        workspaceToEntityRelationshipId = workspaceToEntityRelationship.getIRI();

        Relationship workspaceToUserRelationship = ontologyRepository.getOrCreateRelationshipType(workspaceConcept, rootConcept, WORKSPACE_TO_USER_RELATIONSHIP_NAME, "workspace to user");
        workspaceToUserRelationshipId = workspaceToUserRelationship.getIRI();
    }

    @Override
    public void delete(Workspace workspace, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

        Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, LumifyVisibility.SUPER_USER_VISIBILITY_STRING, workspace.getId());
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, authorizations);
        graph.removeVertex(workspaceVertex, authorizations);
        graph.flush();

        authorizationRepository.removeAuthorizationFromGraph(workspace.getId());
    }

    private Vertex getVertexFromWorkspace(Workspace workspace, Authorizations authorizations) {
        if (workspace instanceof SecureGraphWorkspace) {
            return ((SecureGraphWorkspace) workspace).getVertex(graph, authorizations);
        }
        return graph.getVertex(workspace.getId(), authorizations);
    }

    @Override
    public Workspace findById(String workspaceId, User user) {
        LOGGER.debug("findById(workspaceId: %s, userId: %s)", workspaceId, user.getUserId());
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspaceId);
        Vertex workspaceVertex = graph.getVertex(workspaceId, authorizations);
        if (workspaceVertex == null) {
            return null;
        }
        Workspace workspace = new SecureGraphWorkspace(workspaceVertex);
        if (!doesUserHaveReadAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have read access to workspace " + workspace.getId(), user, workspace.getId());
        }
        return workspace;
    }

    @Override
    public Workspace add(String title, User user) {
        String workspaceId = WORKSPACE_ID_PREFIX + graph.getIdGenerator().nextId();
        authorizationRepository.addAuthorizationToGraph(workspaceId);

        Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, VISIBILITY_STRING, workspaceId);
        Vertex userVertex = graph.getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user: " + user.getUserId());

        VertexBuilder workspaceVertexBuilder = graph.prepareVertex(workspaceId, VISIBILITY.getVisibility(), authorizations);
        OntologyLumifyProperties.CONCEPT_TYPE.setProperty(workspaceVertexBuilder, workspaceConceptId, VISIBILITY.getVisibility());
        WorkspaceLumifyProperties.TITLE.setProperty(workspaceVertexBuilder, title, VISIBILITY.getVisibility());
        Vertex workspaceVertex = workspaceVertexBuilder.save();

        EdgeBuilder edgeBuilder = graph.prepareEdge(workspaceVertex, userVertex, workspaceToUserRelationshipId, VISIBILITY.getVisibility(), authorizations);
        WorkspaceLumifyProperties.WORKSPACE_TO_USER_IS_CREATOR.setProperty(edgeBuilder, true, VISIBILITY.getVisibility());
        WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.setProperty(edgeBuilder, WorkspaceAccess.WRITE.toString(), VISIBILITY.getVisibility());
        edgeBuilder.save();

        graph.flush();
        return new SecureGraphWorkspace(workspaceVertex);
    }

    @Override
    public Iterable<Workspace> findAll(User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
        Iterable<Vertex> vertices = graph.getVertex(user.getUserId(), authorizations).getVertices(Direction.IN, workspaceToUserRelationshipId, authorizations);
        return toWorkspaceIterable(vertices, user);
    }

    @Override
    public void setTitle(Workspace workspace, String title, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user);
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, authorizations);
        WorkspaceLumifyProperties.TITLE.setProperty(workspaceVertex, title, VISIBILITY.getVisibility());
        graph.flush();
    }

    @Override
    public List<WorkspaceUser> findUsersWithAccess(final Workspace workspace, final User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, authorizations);
        Iterable<Edge> userEdges = workspaceVertex.getEdges(Direction.BOTH, workspaceToUserRelationshipId, authorizations);
        return toList(new ConvertingIterable<Edge, WorkspaceUser>(userEdges) {
            @Override
            protected WorkspaceUser convert(Edge edge) {
                String userId = edge.getOtherVertexId(workspace.getId()).toString();

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

    @Override
    public List<WorkspaceEntity> findEntities(final Workspace workspace, User user) {
        LOGGER.debug("findEntities(workspaceId: %s, userId: %s)", workspace.getId(), user.getUserId());
        if (!doesUserHaveReadAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have read access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, authorizations);
        Iterable<Edge> entityEdges = workspaceVertex.getEdges(Direction.BOTH, workspaceToEntityRelationshipId, authorizations);
        return toList(new ConvertingIterable<Edge, WorkspaceEntity>(entityEdges) {
            @Override
            protected WorkspaceEntity convert(Edge edge) {
                Object entityVertexId = edge.getOtherVertexId(workspace.getId());

                int graphPositionX = WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_X.getPropertyValue(edge, 0);
                int graphPositionY = WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y.getPropertyValue(edge, 0);
                boolean visible = WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.getPropertyValue(edge, false);

                return new WorkspaceEntity(entityVertexId, visible, graphPositionX, graphPositionY);
            }
        });
    }

    private Iterable<Edge> findEdges(final Workspace workspace, List<WorkspaceEntity> workspaceEntities, User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());
        Iterable<Vertex> vertices = WorkspaceEntity.toVertices(graph, workspaceEntities, authorizations);
        Iterable<Object> edgeIds = toSet(new VerticesToEdgeIdsIterable(vertices, authorizations));
        final Iterable<Edge> edges = graph.getEdges(edgeIds, authorizations);
        return edges;
    }

    @Override
    public Workspace copy(Workspace workspace, User user) {
        Workspace newWorkspace = add("Copy of " + workspace.getDisplayTitle(), user);

        List<WorkspaceEntity> entities = findEntities(workspace, user);
        for (WorkspaceEntity entity : entities) {
            updateEntityOnWorkspace(newWorkspace, entity.getEntityVertexId(), entity.isVisible(), entity.getGraphPositionX(), entity.getGraphPositionY(), user);
        }

        // TODO should we copy users?

        graph.flush();

        return newWorkspace;
    }

    @Override
    public void softDeleteEntityFromWorkspace(Workspace workspace, Object vertexId, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());
        Vertex otherVertex = graph.getVertex(vertexId, authorizations);
        if (otherVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find vertex: " + vertexId, vertexId);
        }
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, authorizations);
        List<Edge> edges = toList(workspaceVertex.getEdges(otherVertex, Direction.BOTH, authorizations));
        for (Edge edge : edges) {
            WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.setProperty(edge, false, VISIBILITY.getVisibility());
        }
        graph.flush();
    }

    @Override
    public void updateEntityOnWorkspace(Workspace workspace, Object vertexId, Boolean visible, Integer graphPositionX, Integer graphPositionY, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());

        Vertex workspaceVertex = getVertexFromWorkspace(workspace, authorizations);
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
                if (graphPositionX != null && graphPositionY != null) {
                    WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_X.setProperty(m, graphPositionX, VISIBILITY.getVisibility());
                    WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y.setProperty(m, graphPositionY, VISIBILITY.getVisibility());
                }
                if (visible != null) {
                    WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.setProperty(m, visible, VISIBILITY.getVisibility());
                }
                m.save();
            }
        } else {
            EdgeBuilder edgeBuilder = graph.prepareEdge(workspaceVertex, otherVertex, workspaceToEntityRelationshipId, VISIBILITY.getVisibility(), authorizations);
            if (graphPositionX != null && graphPositionY != null) {
                WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_X.setProperty(edgeBuilder, graphPositionX, VISIBILITY.getVisibility());
                WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y.setProperty(edgeBuilder, graphPositionY, VISIBILITY.getVisibility());
            }
            if (visible != null) {
                WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.setProperty(edgeBuilder, visible, VISIBILITY.getVisibility());
            }
            edgeBuilder.save();
        }
        graph.flush();
    }

    @Override
    public void deleteUserFromWorkspace(Workspace workspace, String userId, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, VISIBILITY_STRING, workspace.getId());
        Vertex userVertex = graph.getVertex(userId, authorizations);
        if (userVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find user: " + userId, userId);
        }
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, authorizations);
        List<Edge> edges = toList(workspaceVertex.getEdges(userVertex, Direction.BOTH, workspaceToUserRelationshipId, authorizations));
        for (Edge edge : edges) {
            graph.removeEdge(edge, authorizations);
        }
        graph.flush();
    }

    private boolean doesUserHaveWriteAccess(Workspace workspace, User user) {
        String cacheKey = workspace.getId() + user.getUserId();
        Boolean hasWriteAccess = usersWithWriteAccessCache.getIfPresent(cacheKey);
        if (hasWriteAccess != null && hasWriteAccess) {
            return true;
        }

        List<WorkspaceUser> usersWithAccess = findUsersWithAccess(workspace, user);
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId()) && userWithAccess.getWorkspaceAccess() == WorkspaceAccess.WRITE) {
                usersWithWriteAccessCache.put(cacheKey, true);
                return true;
            }
        }
        return false;
    }

    private boolean doesUserHaveReadAccess(Workspace workspace, User user) {
        String cacheKey = workspace.getId() + user.getUserId();
        Boolean hasReadAccess = usersWithReadAccessCache.getIfPresent(cacheKey);
        if (hasReadAccess != null && hasReadAccess) {
            return true;
        }

        List<WorkspaceUser> usersWithAccess = findUsersWithAccess(workspace, user);
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId())
                    && (userWithAccess.getWorkspaceAccess() == WorkspaceAccess.WRITE || userWithAccess.getWorkspaceAccess() == WorkspaceAccess.READ)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getId());
        Vertex userVertex = graph.getVertex(userId, authorizations);
        if (userVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find user: " + userId, userId);
        }

        Vertex workspaceVertex = getVertexFromWorkspace(workspace, authorizations);
        if (workspaceVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find workspace vertex: " + workspace.getId(), workspace.getId());
        }

        List<Edge> existingEdges = toList(workspaceVertex.getEdges(userVertex, Direction.OUT, workspaceToUserRelationshipId, authorizations));
        if (existingEdges.size() > 0) {
            for (Edge existingEdge : existingEdges) {
                WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.setProperty(existingEdge, workspaceAccess.toString(), VISIBILITY.getVisibility());
            }
        } else {

            EdgeBuilder edgeBuilder = graph.prepareEdge(workspaceVertex, userVertex, workspaceToUserRelationshipId, VISIBILITY.getVisibility(), authorizations);
            WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.setProperty(edgeBuilder, workspaceAccess.toString(), VISIBILITY.getVisibility());
            edgeBuilder.save();
        }

        graph.flush();
    }

    @Override
    public List<DiffItem> getDiff(Workspace workspace, User user) {
        if (!doesUserHaveReadAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

        List<WorkspaceEntity> workspaceEntities = findEntities(workspace, user);
        List<Edge> workspaceEdges = toList(findEdges(workspace, workspaceEntities, user));

        return workspaceDiff.diff(workspace, workspaceEntities, workspaceEdges, user);
    }

    @Override
    public String getCreatorUserId(Workspace workspace, User user) {
        for (WorkspaceUser workspaceUser : findUsersWithAccess(workspace, user)) {
            if (workspaceUser.isCreator()) {
                return workspaceUser.getUserId();
            }
        }
        return null;
    }

    @Override
    public boolean hasWritePermissions(Workspace workspace, User user) {
        for (WorkspaceUser workspaceUser : findUsersWithAccess(workspace, user)) {
            if (workspaceUser.getUserId().equals(user.getUserId())) {
                return workspaceUser.getWorkspaceAccess() == WorkspaceAccess.WRITE;
            }
        }
        return false;
    }

    private Iterable<Workspace> toWorkspaceIterable(Iterable<Vertex> vertices, final User user) {
        return new ConvertingIterable<Vertex, Workspace>(vertices) {
            @Override
            protected Workspace convert(Vertex workspaceVertex) {
                return new SecureGraphWorkspace(workspaceVertex);
            }
        };
    }
}
