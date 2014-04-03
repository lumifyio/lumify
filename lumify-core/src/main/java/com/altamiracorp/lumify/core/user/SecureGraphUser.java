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
    private static final long serialVersionUID = 1L;
    private Vertex user;
    private ModelUserContext modelUserContext;

    private WorkspaceRepository workspaceRepository;
    private UserRepository userRepository;

    public SecureGraphUser(Vertex user, ModelUserContext modelUserContext) {
        this.user = user;
        this.modelUserContext = modelUserContext;
    }

    public String getUserId() {
        return user.getId().toString();
    }

    public ModelUserContext getModelUserContext() {
        return modelUserContext;
    }

    public String getDisplayName() {
        return USERNAME.getPropertyValue(user);
    }

    public Workspace getCurrentWorkspace() {
        return workspaceRepository.findById(CURRENT_WORKSPACE.getPropertyValue(user), userRepository.findById(user.getId().toString()));
    }

    public UserType getUserType() {
        return UserType.USER;
    }

    public Vertex getUser() {
        return user;
    }

    @Override
    public String getUserStatus() {
        return null;
    }

    @Override
    public void setCurrentWorkspace(Workspace currentWorkspace) {
        CURRENT_WORKSPACE.setProperty(user, currentWorkspace.getId(), user.getVisibility());
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
