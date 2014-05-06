package io.lumify.core.model.workspace;

import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.model.workspace.diff.DiffItem;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.user.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.securegraph.util.ConvertingIterable;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkspaceRepository {
    public static String VISIBILITY_STRING = "workspace";
    public static LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    public static String WORKSPACE_CONCEPT_NAME = "http://lumify.io/workspace";
    public static String WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME = "http://lumify.io/workspace/toEntity";
    public static String WORKSPACE_TO_USER_RELATIONSHIP_NAME = "http://lumify.io/workspace/toUser";
    public static String WORKSPACE_ID_PREFIX = "WORKSPACE_";

    // change Workspace to workspace id?
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

    public abstract Workspace add(String title, User user);

    public abstract Iterable<Workspace> findAll(User user);

    // TODO change Workspace to workspace id?
    public abstract void setTitle(Workspace workspace, String title, User user);

    public abstract List<WorkspaceUser> findUsersWithAccess(String workspaceId, User user);

    // TODO change Workspace to workspace id?
    public abstract List<WorkspaceEntity> findEntities(Workspace workspace, User user);

    // TODO change Workspace to workspace id?
    public Workspace copy(Workspace workspace, User user) {
        return copyTo(workspace, user, user);
    }

    // TODO change Workspace to workspace id?
    public Workspace copyTo(Workspace workspace, User destinationUser, User user) {
        Workspace newWorkspace = add("Copy of " + workspace.getDisplayTitle(), destinationUser);
        List<WorkspaceEntity> entities = findEntities(workspace, user);
        for (WorkspaceEntity entity : entities) {
            updateEntityOnWorkspace(newWorkspace, entity.getEntityVertexId(), entity.isVisible(), entity.getGraphPositionX(), entity.getGraphPositionY(), destinationUser);
        }
        return newWorkspace;
    }

    // TODO change Workspace to workspace id?
    public abstract void softDeleteEntityFromWorkspace(Workspace workspace, Object vertexId, User user);

    // TODO change Workspace to workspace id?
    public abstract void updateEntityOnWorkspace(Workspace workspace, Object vertexId, Boolean visible, Integer graphPositionX, Integer graphPositionY, User user);

    // TODO change Workspace to workspace id?
    public abstract void deleteUserFromWorkspace(Workspace workspace, String userId, User user);

    // TODO change Workspace to workspace id?
    public abstract void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user);

    // TODO change Workspace to workspace id?
    public abstract List<DiffItem> getDiff(Workspace workspace, User user);

    public String getCreatorUserId(Workspace workspace, User user) {
        for (WorkspaceUser workspaceUser : findUsersWithAccess(workspace.getId(), user)) {
            if (workspaceUser.isCreator()) {
                return workspaceUser.getUserId();
            }
        }
        return null;
    }

    public abstract boolean hasWritePermissions(String workspaceId, User user);

    public abstract boolean hasReadPermissions(String workspaceId, User user);

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
            workspaceJson.put("isEditable", hasWritePermissions(workspace.getId(), user));

            JSONArray usersJson = new JSONArray();
            for (WorkspaceUser workspaceUser : findUsersWithAccess(workspace.getId(), user)) {
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

                    Integer graphPositionX = workspaceEntity.getGraphPositionX();
                    Integer graphPositionY = workspaceEntity.getGraphPositionY();
                    if (graphPositionX != null && graphPositionY != null) {
                        JSONObject graphPositionJson = new JSONObject();
                        graphPositionJson.put("x", graphPositionX);
                        graphPositionJson.put("y", graphPositionY);
                        workspaceEntityJson.put("graphPosition", graphPositionJson);
                    }

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

