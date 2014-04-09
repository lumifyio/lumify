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

    void delete(Workspace workspace, User user);

    Workspace findById(String workspaceId, User user);

    Workspace add(String title, User user);

    Iterable<Workspace> findAll(User user);

    void setTitle(Workspace workspace, String title, User user);

    List<WorkspaceUser> findUsersWithAccess(Workspace workspace, User user);

    List<WorkspaceEntity> findEntities(String workspaceId, User user);

    List<WorkspaceEntity> findEntities(Workspace workspace, User user);

    Workspace copy(Workspace workspace, User user);

    void deleteEntityFromWorkspace(Workspace workspace, Object vertexId, User user);

    void updateEntityOnWorkspace(Workspace workspace, Object vertexId, boolean visible, Integer graphPositionX, Integer graphPositionY, User user);

    void deleteUserFromWorkspace(Workspace workspace, String userId, User user);

    boolean doesUserHaveWriteAccess(Workspace workspace, User user);

    boolean doesUserHaveReadAccess(Workspace workspace, User user);

    void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user);

    List<DiffItem> getDiff(Workspace workspace, User user);
}

