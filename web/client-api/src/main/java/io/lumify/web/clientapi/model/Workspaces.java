package io.lumify.web.clientapi.model;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class Workspaces {
    private List<Workspace> workspaces = new ArrayList<Workspace>();

    public List<Workspace> getWorkspaces() {
        return workspaces;
    }

    @Override
    public String toString() {
        return "Workspaces{" +
                "workspaces=" + Joiner.on(',').join(workspaces) +
                '}';
    }

    public void addWorkspace(Workspace workspace) {
        this.workspaces.add(workspace);
    }
}

