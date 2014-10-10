package io.lumify.web.clientapi.model;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspaces implements ClientApiObject {
    private List<ClientApiWorkspace> workspaces = new ArrayList<ClientApiWorkspace>();

    public List<ClientApiWorkspace> getWorkspaces() {
        return workspaces;
    }

    @Override
    public String toString() {
        return "Workspaces{" +
                "workspaces=" + Joiner.on(',').join(workspaces) +
                '}';
    }

    public void addWorkspace(ClientApiWorkspace workspace) {
        this.workspaces.add(workspace);
    }
}

