package com.altamiracorp.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserType;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;

import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.CURRENT_WORKSPACE;
import static com.altamiracorp.lumify.core.model.user.UserLumifyProperties.USERNAME;

public class SecureGraphUser implements User {
    private ModelUserContext modelUserContext;
    private String displayName;
    private String userId;
    private WorkspaceRepository workspaceRepository;
    private UserRepository userRepository;

    public SecureGraphUser(String userId, String displayName, ModelUserContext modelUserContext) {
        this.displayName = displayName;
        this.modelUserContext = modelUserContext;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    public String getUserName() {
        return displayName;
    }

    public UserType getUserType() {
        return UserType.USER;
    }

    @Override
    public String getUserStatus() {
        return null;
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
