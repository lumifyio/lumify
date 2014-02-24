package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Workspace {
    private final Vertex workspaceVertex;
    private final User user;
    private final WorkspaceRepository workspaceRepository;
    private List<WorkspaceUser> users;

    Workspace(Vertex workspaceVertex, WorkspaceRepository workspaceRepository, User user) {
        this.workspaceVertex = workspaceVertex;
        this.workspaceRepository = workspaceRepository;
        this.user = user;
    }

    public JSONObject toJson(boolean includeVertices) {
        try {
            ensureUsersLoaded();
            boolean hasWritePermissions = hasWritePermissions(user.getUserId());

            JSONObject workspaceJson = new JSONObject();
            workspaceJson.put("workspaceId", getId());
            workspaceJson.put("title", getTitle());
            workspaceJson.put("createdBy", getCreatorUserId());
            workspaceJson.put("isSharedToUser", !getCreatorUserId().equals(user.getUserId()));
            workspaceJson.put("isEditable", hasWritePermissions);

            JSONArray usersJson = new JSONArray();
            for (WorkspaceUser user : users) {
                String userId = user.getUserId();
                JSONObject userJson = new JSONObject();
                userJson.put("userId", userId);
                userJson.put("access", user.getWorkspaceAccess().toString().toLowerCase());
                usersJson.put(userJson);
            }
            workspaceJson.put("users", usersJson);

            if (includeVertices) {
                List<WorkspaceEntity> workspaceEntities = workspaceRepository.findEntities(this, user);
                JSONObject entitiesJson = new JSONObject();
                for (WorkspaceEntity workspaceEntity : workspaceEntities) {
                    JSONObject workspaceEntityJson = new JSONObject();
                    JSONObject graphPositionJson = new JSONObject();
                    graphPositionJson.put("x", workspaceEntity.getGraphPositionX());
                    graphPositionJson.put("y", workspaceEntity.getGraphPositionY());
                    workspaceEntityJson.put("graphPosition", graphPositionJson);
                    entitiesJson.put(workspaceEntity.getEntityVertexId().toString(), workspaceEntityJson);
                }
                workspaceJson.put("entities", entitiesJson);
            }

            return workspaceJson;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureUsersLoaded() {
        if (users == null) {
            users = workspaceRepository.findUsersWithAccess(this, user);
        }
    }

    public String getId() {
        return this.workspaceVertex.getId().toString();
    }

    public String getCreatorUserId() {
        ensureUsersLoaded();
        for (WorkspaceUser user : users) {
            if (user.isCreator()) {
                return user.getUserId();
            }
        }
        return null;
    }

    public String getTitle() {
        return WorkspaceLumifyProperties.TITLE.getPropertyValue(this.workspaceVertex);
    }

    public Vertex getVertex() {
        return workspaceVertex;
    }

    public static Iterable<Workspace> toWorkspaceIterable(Iterable<Vertex> vertices, final WorkspaceRepository workspaceRepository, final User user) {
        return new ConvertingIterable<Vertex, Workspace>(vertices) {
            @Override
            protected Workspace convert(Vertex vertex) {
                return new Workspace(vertex, workspaceRepository, user);
            }
        };
    }

    public boolean hasWritePermissions(String userId) {
        ensureUsersLoaded();
        for (WorkspaceUser user : users) {
            if (user.getUserId().equals(userId)) {
                return user.getWorkspaceAccess() == WorkspaceAccess.WRITE;
            }
        }
        return false;
    }
}
