package io.lumify.securegraph.model.workspace;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.exception.LumifyResourceNotFoundException;
import io.lumify.core.model.lock.LockRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.ontology.Relationship;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.*;
import io.lumify.core.model.workspace.diff.WorkspaceDiffHelper;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.user.SystemUser;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.securegraph.model.user.SecureGraphUserRepository;
import io.lumify.web.clientapi.model.ClientApiWorkspaceDiff;
import io.lumify.web.clientapi.model.GraphPosition;
import io.lumify.web.clientapi.model.WorkspaceAccess;
import org.elasticsearch.common.base.Function;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.Maps;
import org.securegraph.*;
import org.securegraph.mutation.ElementMutation;
import org.securegraph.util.ConvertingIterable;
import org.securegraph.util.VerticesToEdgeIdsIterable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.toList;
import static org.securegraph.util.IterableUtils.toSet;

@Singleton
public class SecureGraphWorkspaceRepository extends WorkspaceRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SecureGraphWorkspaceRepository.class);
    private String workspaceConceptId;
    private String workspaceToEntityRelationshipId;
    private String workspaceToUserRelationshipId;
    private UserRepository userRepository;
    private AuthorizationRepository authorizationRepository;
    private WorkspaceDiffHelper workspaceDiff;
    private final LockRepository lockRepository;
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
            WorkspaceDiffHelper workspaceDiff,
            LockRepository lockRepository) {
        super(graph);
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
        this.workspaceDiff = workspaceDiff;
        this.lockRepository = lockRepository;

        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);
        authorizationRepository.addAuthorizationToGraph(LumifyVisibility.SUPER_USER_VISIBILITY_STRING);

        Concept rootConcept = ontologyRepository.getConceptByIRI(OntologyRepository.ROOT_CONCEPT_IRI);

        Concept workspaceConcept = ontologyRepository.getOrCreateConcept(null, WORKSPACE_CONCEPT_IRI, "workspace", null);
        workspaceConceptId = workspaceConcept.getTitle();

        ArrayList<Concept> workspaceConceptList = new ArrayList<Concept>();
        workspaceConceptList.add(workspaceConcept);

        ArrayList<Concept> rootConceptList = new ArrayList<Concept>();
        rootConceptList.add(rootConcept);

        Relationship workspaceToEntityRelationship = ontologyRepository.getOrCreateRelationshipType(workspaceConceptList, rootConceptList, WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI, "workspace to entity");
        workspaceToEntityRelationshipId = workspaceToEntityRelationship.getIRI();

        Relationship workspaceToUserRelationship = ontologyRepository.getOrCreateRelationshipType(workspaceConceptList, rootConceptList, WORKSPACE_TO_USER_RELATIONSHIP_IRI, "workspace to user");
        workspaceToUserRelationshipId = workspaceToUserRelationship.getIRI();
    }

    @Override
    public void delete(final Workspace workspace, final User user) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        lockRepository.lock(getLockName(workspace), new Runnable() {
            @Override
            public void run() {
                Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, LumifyVisibility.SUPER_USER_VISIBILITY_STRING, workspace.getWorkspaceId());
                Vertex workspaceVertex = getVertexFromWorkspace(workspace, true, authorizations);
                getGraph().removeVertex(workspaceVertex, authorizations);
                getGraph().flush();

                authorizationRepository.removeAuthorizationFromGraph(workspace.getWorkspaceId());
            }
        });
    }

    private String getLockName(Workspace workspace) {
        return getLockName(workspace.getWorkspaceId());
    }

    private String getLockName(String workspaceId) {
        return "WORKSPACE_" + workspaceId;
    }

    public Vertex getVertex(String workspaceId, User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, LumifyVisibility.SUPER_USER_VISIBILITY_STRING, workspaceId);
        return getGraph().getVertex(workspaceId, authorizations);
    }

    private Vertex getVertexFromWorkspace(Workspace workspace, boolean includeHidden, Authorizations authorizations) {
        if (workspace instanceof SecureGraphWorkspace) {
            return ((SecureGraphWorkspace) workspace).getVertex(getGraph(), includeHidden, authorizations);
        }
        return getGraph().getVertex(workspace.getWorkspaceId(), includeHidden ? FetchHint.ALL_INCLUDING_HIDDEN : FetchHint.ALL, authorizations);
    }

    @Override
    public Workspace findById(String workspaceId, boolean includeHidden, User user) {
        LOGGER.debug("findById(workspaceId: %s, userId: %s)", workspaceId, user.getUserId());
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspaceId);
        Vertex workspaceVertex = getGraph().getVertex(workspaceId, includeHidden ? FetchHint.ALL_INCLUDING_HIDDEN : FetchHint.ALL, authorizations);
        if (workspaceVertex == null) {
            return null;
        }
        if (!hasReadPermissions(workspaceId, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have read access to workspace " + workspaceId, user, workspaceId);
        }
        return new SecureGraphWorkspace(workspaceVertex);
    }

    @Override
    public Workspace add(String workspaceId, String title, User user) {
        authorizationRepository.addAuthorizationToGraph(workspaceId);

        Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, VISIBILITY_STRING, workspaceId);
        Vertex userVertex = getGraph().getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user: " + user.getUserId());

        VertexBuilder workspaceVertexBuilder = getGraph().prepareVertex(workspaceId, VISIBILITY.getVisibility());
        LumifyProperties.CONCEPT_TYPE.setProperty(workspaceVertexBuilder, workspaceConceptId, VISIBILITY.getVisibility());
        WorkspaceLumifyProperties.TITLE.setProperty(workspaceVertexBuilder, title, VISIBILITY.getVisibility());
        Vertex workspaceVertex = workspaceVertexBuilder.save(authorizations);

        addWorkspaceToUser(workspaceVertex, userVertex, authorizations);

        getGraph().flush();
        return new SecureGraphWorkspace(workspaceVertex);
    }

    public void addWorkspaceToUser(Vertex workspaceVertex, Vertex userVertex, Authorizations authorizations) {
        EdgeBuilder edgeBuilder = getGraph().prepareEdge(workspaceVertex, userVertex, workspaceToUserRelationshipId, VISIBILITY.getVisibility());
        WorkspaceLumifyProperties.WORKSPACE_TO_USER_IS_CREATOR.setProperty(edgeBuilder, true, VISIBILITY.getVisibility());
        WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.setProperty(edgeBuilder, WorkspaceAccess.WRITE.toString(), VISIBILITY.getVisibility());
        edgeBuilder.save(authorizations);
    }

    @Override
    public Iterable<Workspace> findAll(User user) {
        checkNotNull(user, "User is required");
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
        Vertex userVertex = getGraph().getVertex(user.getUserId(), authorizations);
        checkNotNull(userVertex, "Could not find user vertex with id " + user.getUserId());
        Iterable<Vertex> vertices = userVertex.getVertices(Direction.IN, workspaceToUserRelationshipId, authorizations);
        return toWorkspaceIterable(vertices, user);
    }

    @Override
    public void setTitle(Workspace workspace, String title, User user) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }
        Authorizations authorizations = userRepository.getAuthorizations(user);
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, false, authorizations);
        WorkspaceLumifyProperties.TITLE.setProperty(workspaceVertex, title, VISIBILITY.getVisibility(), authorizations);
        getGraph().flush();
    }

    @Override
    public List<WorkspaceUser> findUsersWithAccess(final String workspaceId, final User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspaceId);
        Vertex workspaceVertex = getVertex(workspaceId, user);
        Iterable<Edge> userEdges = workspaceVertex.getEdges(Direction.BOTH, workspaceToUserRelationshipId, authorizations);
        return toList(new ConvertingIterable<Edge, WorkspaceUser>(userEdges) {
            @Override
            protected WorkspaceUser convert(Edge edge) {
                String userId = edge.getOtherVertexId(workspaceId);

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
    public List<WorkspaceEntity> findEntities(final Workspace workspace, final User user) {
        LOGGER.debug("findEntities(workspaceId: %s, userId: %s)", workspace.getWorkspaceId(), user.getUserId());
        if (!hasReadPermissions(workspace.getWorkspaceId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have read access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        return lockRepository.lock(getLockName(workspace), new Callable<List<WorkspaceEntity>>() {
            @Override
            public List<WorkspaceEntity> call() throws Exception {
                return findEntitiesNoLock(workspace, false, user);
            }
        });
    }

    public List<WorkspaceEntity> findEntitiesNoLock(final Workspace workspace, boolean includeHidden, User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getWorkspaceId());
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, includeHidden, authorizations);
        Iterable<Edge> entityEdges = workspaceVertex.getEdges(Direction.BOTH, workspaceToEntityRelationshipId, authorizations);
        return toList(new ConvertingIterable<Edge, WorkspaceEntity>(entityEdges) {
            @Override
            protected WorkspaceEntity convert(Edge edge) {
                String entityVertexId = edge.getOtherVertexId(workspace.getWorkspaceId());

                Integer graphPositionX = WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_X.getPropertyValue(edge);
                Integer graphPositionY = WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y.getPropertyValue(edge);
                boolean visible = WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.getPropertyValue(edge, false);

                return new WorkspaceEntity(entityVertexId, visible, graphPositionX, graphPositionY);
            }
        });
    }

    private Iterable<Edge> findEdges(final Workspace workspace, List<WorkspaceEntity> workspaceEntities, boolean includeHidden, User user) {
        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getWorkspaceId());
        Iterable<Vertex> vertices = WorkspaceEntity.toVertices(getGraph(), workspaceEntities, includeHidden, authorizations);
        Iterable<String> edgeIds = toSet(new VerticesToEdgeIdsIterable(vertices, authorizations));
        return getGraph().getEdges(edgeIds, includeHidden ? FetchHint.ALL_INCLUDING_HIDDEN : FetchHint.ALL, authorizations);
    }

    @Override
    public Workspace copyTo(Workspace workspace, User destinationUser, User user) {
        Workspace newWorkspace = super.copyTo(workspace, destinationUser, user);
        getGraph().flush();
        return newWorkspace;
    }

    @Override
    public void softDeleteEntityFromWorkspace(Workspace workspace, String vertexId, User user) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getWorkspaceId());
        Vertex otherVertex = getGraph().getVertex(vertexId, authorizations);
        if (otherVertex == null) {
            throw new LumifyResourceNotFoundException("Could not find vertex: " + vertexId, vertexId);
        }
        Vertex workspaceVertex = getVertexFromWorkspace(workspace, true, authorizations);
        List<Edge> edges = toList(workspaceVertex.getEdges(otherVertex, Direction.BOTH, authorizations));
        for (Edge edge : edges) {
            WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.setProperty(edge, false, VISIBILITY.getVisibility(), authorizations);
        }
        getGraph().flush();
    }

    @Override
    public void updateEntitiesOnWorkspace(final Workspace workspace, final Iterable<Update> updates, final User user) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        lockRepository.lock(getLockName(workspace.getWorkspaceId()), new Runnable() {
            @Override
            public void run() {
                Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getWorkspaceId());

                Vertex workspaceVertex = getVertexFromWorkspace(workspace, true, authorizations);
                if (workspaceVertex == null) {
                    throw new LumifyResourceNotFoundException("Could not find workspace vertex: " + workspace.getWorkspaceId(), workspace.getWorkspaceId());
                }

                Iterable<String> vertexIds = new ConvertingIterable<Update, String>(updates) {
                    @Override
                    protected String convert(Update o) {
                        return o.getVertexId();
                    }
                };
                Iterable<Vertex> vertices = getGraph().getVertices(vertexIds, authorizations);
                ImmutableMap<String, Vertex> verticesMap = Maps.uniqueIndex(vertices, new Function<Vertex, String>() {
                    @Override
                    public String apply(Vertex vertex) {
                        return vertex.getId();
                    }
                });

                for (Update update : updates) {
                    Vertex otherVertex = verticesMap.get(update.getVertexId());
                    checkNotNull(otherVertex, "Could not find vertex with id: " + update.getVertexId());
                    createEdge(workspaceVertex, otherVertex, update.getGraphPosition(), update.getVisible(), authorizations);
                }
                getGraph().flush();
            }
        });
    }

    private void createEdge(Vertex workspaceVertex, Vertex otherVertex, GraphPosition graphPosition, Boolean visible, Authorizations authorizations) {
        List<Edge> existingEdges = toList(workspaceVertex.getEdges(otherVertex, Direction.BOTH, authorizations));
        if (existingEdges.size() > 0) {
            for (Edge existingEdge : existingEdges) {
                ElementMutation<Edge> m = existingEdge.prepareMutation();
                if (graphPosition != null) {
                    WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_X.setProperty(m, graphPosition.getX(), VISIBILITY.getVisibility());
                    WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y.setProperty(m, graphPosition.getY(), VISIBILITY.getVisibility());
                }
                if (visible != null) {
                    WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.setProperty(m, visible, VISIBILITY.getVisibility());
                }
                m.save(authorizations);
            }
        } else {
            EdgeBuilder edgeBuilder = getGraph().prepareEdge(workspaceVertex, otherVertex, workspaceToEntityRelationshipId, VISIBILITY.getVisibility());
            if (graphPosition != null) {
                WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_X.setProperty(edgeBuilder, graphPosition.getX(), VISIBILITY.getVisibility());
                WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_GRAPH_POSITION_Y.setProperty(edgeBuilder, graphPosition.getY(), VISIBILITY.getVisibility());
            }
            if (visible != null) {
                WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.setProperty(edgeBuilder, visible, VISIBILITY.getVisibility());
            }
            edgeBuilder.save(authorizations);
        }
    }

    @Override
    public void deleteUserFromWorkspace(final Workspace workspace, final String userId, final User user) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        lockRepository.lock(getLockName(workspace), new Runnable() {
            @Override
            public void run() {
                Authorizations authorizations = userRepository.getAuthorizations(user, UserRepository.VISIBILITY_STRING, VISIBILITY_STRING, workspace.getWorkspaceId());
                Vertex userVertex = getGraph().getVertex(userId, authorizations);
                if (userVertex == null) {
                    throw new LumifyResourceNotFoundException("Could not find user: " + userId, userId);
                }
                Vertex workspaceVertex = getVertexFromWorkspace(workspace, true, authorizations);
                List<Edge> edges = toList(workspaceVertex.getEdges(userVertex, Direction.BOTH, workspaceToUserRelationshipId, authorizations));
                for (Edge edge : edges) {
                    getGraph().removeEdge(edge, authorizations);
                }
                getGraph().flush();

                usersWithWriteAccessCache.invalidateAll();
                usersWithReadAccessCache.invalidateAll();
            }
        });
    }

    @Override
    public boolean hasWritePermissions(String workspaceId, User user) {
        if (user instanceof SystemUser) {
            return true;
        }

        String cacheKey = workspaceId + user.getUserId();
        Boolean hasWriteAccess = usersWithWriteAccessCache.getIfPresent(cacheKey);
        if (hasWriteAccess != null && hasWriteAccess) {
            return true;
        }

        List<WorkspaceUser> usersWithAccess = findUsersWithAccess(workspaceId, user);
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId()) && userWithAccess.getWorkspaceAccess() == WorkspaceAccess.WRITE) {
                usersWithWriteAccessCache.put(cacheKey, true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasReadPermissions(String workspaceId, User user) {
        if (user instanceof SystemUser) {
            return true;
        }

        String cacheKey = workspaceId + user.getUserId();
        Boolean hasReadAccess = usersWithReadAccessCache.getIfPresent(cacheKey);
        if (hasReadAccess != null && hasReadAccess) {
            return true;
        }

        List<WorkspaceUser> usersWithAccess = findUsersWithAccess(workspaceId, user);
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user.getUserId())
                    && (userWithAccess.getWorkspaceAccess() == WorkspaceAccess.WRITE || userWithAccess.getWorkspaceAccess() == WorkspaceAccess.READ)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateUserOnWorkspace(final Workspace workspace, final String userId, final WorkspaceAccess workspaceAccess, final User user) {
        if (!hasWritePermissions(workspace.getWorkspaceId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        lockRepository.lock(getLockName(workspace), new Runnable() {
            @Override
            public void run() {
                Authorizations authorizations = userRepository.getAuthorizations(user, VISIBILITY_STRING, workspace.getWorkspaceId());
                Vertex otherUserVertex;
                if (userRepository instanceof SecureGraphUserRepository) {
                    otherUserVertex = ((SecureGraphUserRepository) userRepository).findByIdUserVertex(userId);
                } else {
                    otherUserVertex = getGraph().getVertex(userId, authorizations);
                }
                if (otherUserVertex == null) {
                    throw new LumifyResourceNotFoundException("Could not find user: " + userId, userId);
                }

                Vertex workspaceVertex = getVertexFromWorkspace(workspace, true, authorizations);
                if (workspaceVertex == null) {
                    throw new LumifyResourceNotFoundException("Could not find workspace vertex: " + workspace.getWorkspaceId(), workspace.getWorkspaceId());
                }

                List<Edge> existingEdges = toList(workspaceVertex.getEdges(otherUserVertex, Direction.OUT, workspaceToUserRelationshipId, authorizations));
                if (existingEdges.size() > 0) {
                    for (Edge existingEdge : existingEdges) {
                        WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.setProperty(existingEdge, workspaceAccess.toString(), VISIBILITY.getVisibility(), authorizations);
                    }
                } else {

                    EdgeBuilder edgeBuilder = getGraph().prepareEdge(workspaceVertex, otherUserVertex, workspaceToUserRelationshipId, VISIBILITY.getVisibility());
                    WorkspaceLumifyProperties.WORKSPACE_TO_USER_ACCESS.setProperty(edgeBuilder, workspaceAccess.toString(), VISIBILITY.getVisibility());
                    edgeBuilder.save(authorizations);
                }

                getGraph().flush();

                usersWithReadAccessCache.invalidateAll();
                usersWithWriteAccessCache.invalidateAll();
            }
        });
    }

    @Override
    public ClientApiWorkspaceDiff getDiff(final Workspace workspace, final User user) {
        if (!hasReadPermissions(workspace.getWorkspaceId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getWorkspaceId(), user, workspace.getWorkspaceId());
        }

        return lockRepository.lock(getLockName(workspace), new Callable<ClientApiWorkspaceDiff>() {
            @Override
            public ClientApiWorkspaceDiff call() throws Exception {
                List<WorkspaceEntity> workspaceEntities = findEntitiesNoLock(workspace, true, user);
                List<Edge> workspaceEdges = toList(findEdges(workspace, workspaceEntities, true, user));

                return workspaceDiff.diff(workspace, workspaceEntities, workspaceEdges, user);
            }
        });
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
