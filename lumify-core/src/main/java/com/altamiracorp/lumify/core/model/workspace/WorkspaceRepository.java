package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.model.workspace.diff.DiffItem;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;

import java.util.List;
import java.util.Map;

public interface WorkspaceRepository {
    String VISIBILITY_STRING = "workspace";
    LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    String WORKSPACE_CONCEPT_NAME = "http://lumify.io/workspace";
    String WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME = "http://lumify.io/workspace/toEntity";
    String WORKSPACE_TO_USER_RELATIONSHIP_NAME = "http://lumify.io/workspace/toUser";
    String WORKSPACE_ID_PREFIX = "WORKSPACE_";

    void init(Map map);

    // change Workspace to workspace id?
    void delete(Workspace workspace, User user);

    Workspace findById(String workspaceId, User user);

    Workspace add(String title, User user);

    Iterable<Workspace> findAll(User user);

    // change Workspace to workspace id?
    void setTitle(Workspace workspace, String title, User user);

    // change Workspace to workspace id?
    List<WorkspaceUser> findUsersWithAccess(Workspace workspace, User user);

    List<WorkspaceEntity> findEntities(String workspaceId, User user);

    // change Workspace to workspace id?
    List<WorkspaceEntity> findEntities(Workspace workspace, User user);

    // change Workspace to workspace id?
    Workspace copy(Workspace workspace, User user);

    // change Workspace to workspace id?
    void deleteEntityFromWorkspace(Workspace workspace, Object vertexId, User user);

    // change Workspace to workspace id?
    void updateEntityOnWorkspace(Workspace workspace, Object vertexId, Boolean visible, Integer graphPositionX, Integer graphPositionY, User user);

    // change Workspace to workspace id?
    void deleteUserFromWorkspace(Workspace workspace, String userId, User user);

    // TODO not used can we get rid of
    boolean doesUserHaveWriteAccess(Workspace workspace, User user);

    // TODO not used can we get rid of
    boolean doesUserHaveReadAccess(Workspace workspace, User user);

    // change Workspace to workspace id?
    void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user);

    // change Workspace to workspace id?
    List<DiffItem> getDiff(Workspace workspace, User user);

    // change Workspace to workspace id?
    String getCreatorUserId(Workspace workspace, User user);

    // change Workspace to workspace id?
    boolean hasWritePermissions(Workspace workspace, User user);
}

