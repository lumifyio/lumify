package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.model.workspace.diff.DiffItem;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkspaceRepository {
    public static String VISIBILITY_STRING = "workspace";
    public static LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    public static String WORKSPACE_CONCEPT_NAME = "http://lumify.io/workspace";
    public static String WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME = "http://lumify.io/workspace/toEntity";
    public static String WORKSPACE_TO_USER_RELATIONSHIP_NAME = "http://lumify.io/workspace/toUser";
    public static String WORKSPACE_ID_PREFIX = "WORKSPACE_";

    public abstract void init(Map map);

    // change Workspace to workspace id?
    public abstract void delete(Workspace workspace, User user);

    public abstract Workspace findById(String workspaceId, User user);

    public abstract Workspace add(String title, User user);

    public abstract Iterable<Workspace> findAll(User user);

    // change Workspace to workspace id?
    public abstract void setTitle(Workspace workspace, String title, User user);

    // change Workspace to workspace id?
    public abstract List<WorkspaceUser> findUsersWithAccess(Workspace workspace, User user);

    // change Workspace to workspace id?
    public abstract List<WorkspaceEntity> findEntities(Workspace workspace, User user);

    // change Workspace to workspace id?
    public abstract Workspace copy(Workspace workspace, User user);

    // change Workspace to workspace id?
    public abstract void softDeleteEntityFromWorkspace(Workspace workspace, Object vertexId, User user);

    // change Workspace to workspace id?
    public abstract void updateEntityOnWorkspace(Workspace workspace, Object vertexId, Boolean visible, Integer graphPositionX, Integer graphPositionY, User user);

    // change Workspace to workspace id?
    public abstract void deleteUserFromWorkspace(Workspace workspace, String userId, User user);

    // change Workspace to workspace id?
    public abstract void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user);

    // change Workspace to workspace id?
    public abstract List<DiffItem> getDiff(Workspace workspace, User user);

    // change Workspace to workspace id?
    public abstract String getCreatorUserId(Workspace workspace, User user);

    // change Workspace to workspace id?
    public abstract boolean hasWritePermissions(Workspace workspace, User user);

    public JSONObject toJson(Workspace workspace, User user, boolean includeVertices) {
        checkNotNull(workspace, "workspace cannot be null");
        checkNotNull(user, "user cannot be null");

        try {
            JSONObject workspaceJson = new JSONObject();
            workspaceJson.put("workspaceId", workspace.getId());
            workspaceJson.put("title", workspace.getDisplayTitle());

            String creatorUserId = getCreatorUserId(workspace, user);
            if (creatorUserId != null) {
                workspaceJson.put("createdBy", creatorUserId);
                workspaceJson.put("isSharedToUser", !creatorUserId.equals(user.getUserId()));
            }
            workspaceJson.put("isEditable", hasWritePermissions(workspace, user));

            JSONArray usersJson = new JSONArray();
            for (WorkspaceUser workspaceUser : findUsersWithAccess(workspace, user)) {
                String userId = workspaceUser.getUserId();
                JSONObject userJson = new JSONObject();
                userJson.put("userId", userId);
                userJson.put("access", workspaceUser.getWorkspaceAccess().toString().toLowerCase());
                usersJson.put(userJson);
            }
            workspaceJson.put("users", usersJson);

            if (includeVertices) {
                JSONObject entitiesJson = new JSONObject();
                for (WorkspaceEntity workspaceEntity : findEntities(workspace, user)) {
                    if (!workspaceEntity.isVisible()) {
                        continue;
                    }
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
}

