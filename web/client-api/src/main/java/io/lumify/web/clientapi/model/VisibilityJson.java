package io.lumify.web.clientapi.model;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class VisibilityJson {
    private String source;
    private List<String> workspaces = new ArrayList<String>();

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getWorkspaces() {
        return workspaces;
    }

    public void addWorkspace(String workspaceId) {
        workspaces.add(workspaceId);
    }

    @Override
    public String toString() {
        return "VisibilityJson{" +
                "source='" + source + '\'' +
                ", workspaces=" + Joiner.on(',').join(workspaces) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VisibilityJson that = (VisibilityJson) o;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (workspaces != null ? !workspaces.equals(that.workspaces) : that.workspaces != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }
}
