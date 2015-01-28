package io.lumify.core.model.workspace;

import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.user.User;
import io.lumify.web.clientapi.model.ClientApiWorkspace;
import io.lumify.web.clientapi.model.ClientApiWorkspaceDiff;
import io.lumify.web.clientapi.model.GraphPosition;
import io.lumify.web.clientapi.model.WorkspaceAccess;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.securegraph.Graph;
import org.securegraph.util.ConvertingIterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkspaceRepository {
    public static final String VISIBILITY_STRING = "workspace";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    public static final String WORKSPACE_CONCEPT_IRI = "http://lumify.io/workspace#workspace";
    public static final String WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI = "http://lumify.io/workspace/toEntity";
    public static final String WORKSPACE_TO_USER_RELATIONSHIP_IRI = "http://lumify.io/workspace/toUser";
    public static final String WORKSPACE_ID_PREFIX = "WORKSPACE_";
    public static final String OWL_IRI = "http://lumify.io/workspace";
    private final Graph graph;

    protected WorkspaceRepository(Graph graph) {
        this.graph = graph;
    }

    public abstract void delete(Workspace workspace, User user);

    public abstract Workspace findById(String workspaceId, User user);

    public Iterable<Workspace> findByIds(final Iterable<String> workspaceIds, final User user) {
        return new ConvertingIterable<String, Workspace>(workspaceIds) {
            @Override
            protected Workspace convert(String workspaceId) {
                if (workspaceId == null) {
                    return null;
                }
                try {
                    return findById(workspaceId, user);
                } catch (LumifyAccessDeniedException ex) {
                    return null;
                }
            }
        };
    }

    public abstract Workspace add(String workspaceId, String title, User user);

    public Workspace add(String title, User user) {
        String workspaceId = WORKSPACE_ID_PREFIX + graph.getIdGenerator().nextId();
        return add(workspaceId, title, user);
    }

    public abstract Iterable<Workspace> findAll(User user);

    public abstract void setTitle(Workspace workspace, String title, User user);

    public abstract List<WorkspaceUser> findUsersWithAccess(String workspaceId, User user);

    public abstract List<WorkspaceEntity> findEntities(Workspace workspace, User user);

    public Workspace copy(Workspace workspace, User user) {
        return copyTo(workspace, user, user);
    }

    public Workspace copyTo(Workspace workspace, User destinationUser, User user) {
        Workspace newWorkspace = add("Copy of " + workspace.getDisplayTitle(), destinationUser);
        List<WorkspaceEntity> entities = findEntities(workspace, user);
        Iterable<Update> updates = new ConvertingIterable<WorkspaceEntity, Update>(entities) {
            @Override
            protected Update convert(WorkspaceEntity entity) {
                return new Update(entity.getEntityVertexId(), entity.isVisible(), new GraphPosition(entity.getGraphPositionX(), entity.getGraphPositionY()));
            }
        };
        updateEntitiesOnWorkspace(newWorkspace, updates, destinationUser);
        return newWorkspace;
    }

    public abstract void softDeleteEntityFromWorkspace(Workspace workspace, String vertexId, User user);

    public abstract void deleteUserFromWorkspace(Workspace workspace, String userId, User user);

    public abstract void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user);

    public abstract ClientApiWorkspaceDiff getDiff(Workspace workspace, User user, Locale locale, String timeZone);

    public String getCreatorUserId(Workspace workspace, User user) {
        for (WorkspaceUser workspaceUser : findUsersWithAccess(workspace.getWorkspaceId(), user)) {
            if (workspaceUser.isCreator()) {
                return workspaceUser.getUserId();
            }
        }
        return null;
    }

    public abstract boolean hasCommentPermissions(String workspaceId, User user);

    public abstract boolean hasWritePermissions(String workspaceId, User user);

    public abstract boolean hasReadPermissions(String workspaceId, User user);

    public JSONArray toJson(Iterable<Workspace> workspaces, User user, boolean includeVertices) {
        JSONArray resultJson = new JSONArray();
        for (Workspace workspace : workspaces) {
            resultJson.put(toJson(workspace, user, includeVertices));
        }
        return resultJson;
    }

    public JSONObject toJson(Workspace workspace, User user, boolean includeVertices) {
        checkNotNull(workspace, "workspace cannot be null");
        checkNotNull(user, "user cannot be null");

        try {
            JSONObject workspaceJson = new JSONObject();
            workspaceJson.put("workspaceId", workspace.getWorkspaceId());
            workspaceJson.put("title", workspace.getDisplayTitle());

            String creatorUserId = getCreatorUserId(workspace, user);
            if (creatorUserId != null) {
                workspaceJson.put("createdBy", creatorUserId);
                workspaceJson.put("sharedToUser", !creatorUserId.equals(user.getUserId()));
            }
            workspaceJson.put("editable", hasWritePermissions(workspace.getWorkspaceId(), user));

            JSONArray usersJson = new JSONArray();
            for (WorkspaceUser workspaceUser : findUsersWithAccess(workspace.getWorkspaceId(), user)) {
                String userId = workspaceUser.getUserId();
                JSONObject userJson = new JSONObject();
                userJson.put("userId", userId);
                userJson.put("access", workspaceUser.getWorkspaceAccess().toString().toLowerCase());
                usersJson.put(userJson);
            }
            workspaceJson.put("users", usersJson);

            if (includeVertices) {
                JSONArray verticesJson = new JSONArray();
                for (WorkspaceEntity workspaceEntity : findEntities(workspace, user)) {
                    if (!workspaceEntity.isVisible()) {
                        continue;
                    }

                    JSONObject vertexJson = new JSONObject();
                    vertexJson.put("vertexId", workspaceEntity.getEntityVertexId());

                    Integer graphPositionX = workspaceEntity.getGraphPositionX();
                    Integer graphPositionY = workspaceEntity.getGraphPositionY();
                    if (graphPositionX != null && graphPositionY != null) {
                        JSONObject graphPositionJson = new JSONObject();
                        graphPositionJson.put("x", graphPositionX);
                        graphPositionJson.put("y", graphPositionY);
                        vertexJson.put("graphPosition", graphPositionJson);
                    }

                    verticesJson.put(vertexJson);
                }
                workspaceJson.put("vertices", verticesJson);
            }

            return workspaceJson;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public ClientApiWorkspace toClientApi(Workspace workspace, User user, boolean includeVertices) {
        checkNotNull(workspace, "workspace cannot be null");
        checkNotNull(user, "user cannot be null");

        try {
            ClientApiWorkspace workspaceClientApi = new ClientApiWorkspace();
            workspaceClientApi.setWorkspaceId(workspace.getWorkspaceId());
            workspaceClientApi.setTitle(workspace.getDisplayTitle());

            String creatorUserId = getCreatorUserId(workspace, user);
            if (creatorUserId != null) {
                workspaceClientApi.setCreatedBy(creatorUserId);
                workspaceClientApi.setSharedToUser(!creatorUserId.equals(user.getUserId()));
            }
            workspaceClientApi.setEditable(hasWritePermissions(workspace.getWorkspaceId(), user));
            workspaceClientApi.setCommentable(hasCommentPermissions(workspace.getWorkspaceId(), user));

            for (WorkspaceUser u : findUsersWithAccess(workspace.getWorkspaceId(), user)) {
                String userId = u.getUserId();
                ClientApiWorkspace.User workspaceUser = new ClientApiWorkspace.User();
                workspaceUser.setUserId(userId);
                workspaceUser.setAccess(u.getWorkspaceAccess());
                workspaceClientApi.addUser(workspaceUser);
            }

            if (includeVertices) {
                for (WorkspaceEntity workspaceEntity : findEntities(workspace, user)) {
                    if (!workspaceEntity.isVisible()) {
                        continue;
                    }

                    ClientApiWorkspace.Vertex v = new ClientApiWorkspace.Vertex();
                    v.setVertexId(workspaceEntity.getEntityVertexId());

                    Integer graphPositionX = workspaceEntity.getGraphPositionX();
                    Integer graphPositionY = workspaceEntity.getGraphPositionY();
                    if (graphPositionX != null && graphPositionY != null) {
                        GraphPosition graphPosition = new GraphPosition(graphPositionX, graphPositionY);
                        v.setGraphPosition(graphPosition);
                        v.setGraphLayoutJson(null);
                    } else {
                        v.setGraphPosition(null);

                        String graphLayoutJson = workspaceEntity.getGraphLayoutJson();
                        if (graphLayoutJson != null) {
                            v.setGraphLayoutJson(graphLayoutJson);
                        } else {
                            v.setGraphLayoutJson(null);
                        }
                    }

                    workspaceClientApi.addVertex(v);
                }
            } else {
                workspaceClientApi.removeVertices();
            }

            return workspaceClientApi;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected Graph getGraph() {
        return graph;
    }

    public abstract void updateEntitiesOnWorkspace(Workspace workspace, Iterable<Update> updates, User user);

    public void updateEntityOnWorkspace(Workspace workspace, Update update, User user) {
        List<Update> updates = new ArrayList<Update>();
        updates.add(update);
        updateEntitiesOnWorkspace(workspace, updates, user);
    }

    public void updateEntityOnWorkspace(Workspace workspace, String vertexId, Boolean visible, GraphPosition graphPosition, User user) {
        updateEntityOnWorkspace(workspace, new Update(vertexId, visible, graphPosition), user);
    }

    public void updateEntityOnWorkspace(String workspaceId, String vertexId, Boolean visible, GraphPosition graphPosition, User user) {
        Workspace workspace = findById(workspaceId, user);
        updateEntityOnWorkspace(workspace, vertexId, visible, graphPosition, user);
    }

    public static class Update {
        private final String vertexId;
        private final Boolean visible;
        private final GraphPosition graphPosition;
        private final String graphLayoutJson;

        public Update(String vertexId, Boolean visible, GraphPosition graphPosition) {
            this.vertexId = vertexId;
            this.visible = visible;
            this.graphPosition = graphPosition;
            graphLayoutJson = null;
        }

        public Update(String vertexId, Boolean visible, GraphPosition graphPosition, String graphLayoutJson) {
            this.vertexId = vertexId;
            this.visible = visible;
            this.graphPosition = graphPosition;
            this.graphLayoutJson = graphLayoutJson;
        }

        public String getVertexId() {
            return vertexId;
        }

        public Boolean getVisible() {
            return visible;
        }

        public GraphPosition getGraphPosition() {
            return graphPosition;
        }

        public String getGraphLayoutJson() {
            return graphLayoutJson;
        }
    }
}

