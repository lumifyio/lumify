package io.lumify.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Joiner;
import io.lumify.web.clientapi.model.util.ClientApiConverter;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;

import java.util.HashSet;
import java.util.Set;

public class VisibilityJson {
    private String source = "";
    private Set<String> workspaces = new HashSet<>();

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Set<String> getWorkspaces() {
        return workspaces;
    }

    public void addWorkspace(String workspaceId) {
        workspaces.add(workspaceId);
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
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
