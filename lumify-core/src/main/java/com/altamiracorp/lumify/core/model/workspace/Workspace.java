package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Workspace {
    private final Vertex workspaceVertex;
    private final User user;
    private final WorkspaceRepository workspaceRepository;
    private List<Vertex> users;

    Workspace(Vertex workspaceVertex, WorkspaceRepository workspaceRepository, User user) {
        this.workspaceVertex = workspaceVertex;
        this.workspaceRepository = workspaceRepository;
        this.user = user;
    }

    public JSONObject toJson(boolean includeReferences) {
        try {
            ensureUsersLoaded();

            JSONObject workspaceJson = new JSONObject();
            workspaceJson.put("id", getId());
            workspaceJson.put("title", getTitle());
            workspaceJson.put("createdBy", getCreatorUserId());
            workspaceJson.put("isSharedToUser", !getCreatorUserId().equals(user.getUserId()));

            Boolean hasEdit = false;

            if (getCreatorUserId().equals(user.getUserId())) {
                hasEdit = true;
            }

            // TODO send back permissions
//            JSONArray permissions = new JSONArray();
//            if (get(WorkspacePermissions.NAME) != null) {
//                for (Column col : get(WorkspacePermissions.NAME).getColumns()) {
//                    String userId = col.getName();
//                    JSONObject users = new JSONObject();
//                    JSONObject userPermissions = Value.toJson(col.getValue());
//                    users.put("user", userId);
//                    users.put("userPermissions", userPermissions);
//                    permissions.put(users);
//                    if (userId.equals(user.getUserId())) {
//                        if (userPermissions.getBoolean("edit")) {
//                            hasEdit = true;
//                        }
//                        hasAccess = true;
//                    }
//                }
//
//                workspaceJson.put("permissions", permissions);
//            }

            workspaceJson.put("isEditable", hasEdit);
            return workspaceJson;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureUsersLoaded() {
        if (users == null) {
            users = workspaceRepository.findUsersWithAccess(workspaceVertex, user);
        }
    }

    public String getId() {
        return this.workspaceVertex.getId().toString();
    }

    public String getCreatorUserId() {
        ensureUsersLoaded();
        return this.users.get(0).getId().toString();
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
}
