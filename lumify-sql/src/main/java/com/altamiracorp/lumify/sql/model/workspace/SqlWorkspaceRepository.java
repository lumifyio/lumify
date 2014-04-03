package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.core.model.workspace.*;
import com.altamiracorp.lumify.core.model.workspace.diff.DiffItem;
import com.altamiracorp.lumify.core.user.User;

import java.util.List;
import java.util.Map;

public class SqlWorkspaceRepository implements WorkspaceRepository {
    @Override
    public void init(Map map) {

    }

    @Override
    public void delete(Workspace workspace, User user) {
        throw new RuntimeException("Not yet Implemented");
    }

    @Override
    public Workspace findById(String workspaceId, User user) {
        throw new RuntimeException("Not yet Implemented");
    }

    @Override
    public Workspace add(String title, User user) {
        throw new RuntimeException("Not yet Implemented");
    }

    @Override
    public Iterable<Workspace> findAll(User user) {
        throw new RuntimeException("Not yet Implemented");
    }

    @Override
    public void setTitle(Workspace workspace, String title, User user) {

    }

    @Override
    public List<WorkspaceUser> findUsersWithAccess(Workspace workspace, User user) {
        throw new RuntimeException("Not yet Implemented");
    }

    @Override
    public List<WorkspaceEntity> findEntities(String workspaceId, User user) {
        throw new RuntimeException("Not yet Implemented");
    }

    @Override
    public List<WorkspaceEntity> findEntities(Workspace workspace, User user) {
        throw new RuntimeException("Not yet Implemented");
    }

    @Override
    public Workspace copy(Workspace workspace, User user) {
        throw new RuntimeException("Not yet Implemented");
    }

    @Override
    public void deleteEntityFromWorkspace(Workspace workspace, Object vertexId, User user) {

    }

    @Override
    public void updateEntityOnWorkspace(Workspace workspace, Object vertexId, boolean visible, int graphPositionX, int graphPositionY, User user) {

    }

    @Override
    public void deleteUserFromWorkspace(Workspace workspace, String userId, User user) {

    }

    @Override
    public boolean doesUserHaveWriteAccess(Workspace workspace, User user) {
        return false;
    }

    @Override
    public boolean doesUserHaveReadAccess(Workspace workspace, User user) {
        return false;
    }

    @Override
    public void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user) {

    }

    @Override
    public List<DiffItem> getDiff(Workspace workspace, User user) {
        throw new RuntimeException("Not yet Implemented");
    }
}
