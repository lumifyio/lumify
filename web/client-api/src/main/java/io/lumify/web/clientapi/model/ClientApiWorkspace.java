package io.lumify.web.clientapi.model;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspace implements ClientApiObject {
    private String workspaceId;
    private String title;
    private String createdBy;
    private boolean isSharedToUser;
    private boolean isEditable;
    private boolean isCommentable;
    private List<User> users = new ArrayList<User>();
    private List<Vertex> vertices = new ArrayList<Vertex>();
    private boolean active;

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isSharedToUser() {
        return isSharedToUser;
    }

    public void setSharedToUser(boolean isSharedToUser) {
        this.isSharedToUser = isSharedToUser;
    }

    public boolean isEditable() {
        return isEditable;
    }

    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }

    public boolean isCommentable() {
        return isCommentable;
    }

    public void setCommentable(boolean isCommentable) {
        this.isCommentable = isCommentable;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public void addUser(User user) {
        this.users.add(user);
    }

    public void addVertex(Vertex vertex) {
        this.vertices.add(vertex);
    }

    public void removeVertices() {
        this.vertices = null;
    }

    @Override
    public String toString() {
        return "Workspace{" +
                "workspaceId='" + workspaceId + '\'' +
                ", title='" + title + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", isSharedToUser=" + isSharedToUser +
                ", isEditable=" + isEditable +
                ", active=" + active +
                ", users=" + Joiner.on(',').join(users) +
                ", vertices=" + Joiner.on(',').join(vertices) +
                '}';
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public static class Vertex {
        private String vertexId;
        private GraphPosition graphPosition = new GraphPosition();
        private String graphLayoutJson;

        public String getVertexId() {
            return vertexId;
        }

        public void setVertexId(String vertexId) {
            this.vertexId = vertexId;
        }

        public GraphPosition getGraphPosition() {
            return graphPosition;
        }

        public void setGraphPosition(GraphPosition graphPosition) {
            this.graphPosition = graphPosition;
        }

        public String getGraphLayoutJson() {
            return graphLayoutJson;
        }

        public void setGraphLayoutJson(String graphLayoutJson) {
            this.graphLayoutJson = graphLayoutJson;
        }

        @Override
        public String toString() {
            return "Vertex{" +
                    "vertexId='" + vertexId + '\'' +
                    ", graphPosition=" + graphPosition +
                    '}';
        }
    }

    public static class User {
        private String userId;
        private WorkspaceAccess access;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public WorkspaceAccess getAccess() {
            return access;
        }

        public void setAccess(WorkspaceAccess access) {
            this.access = access;
        }

        @Override
        public String toString() {
            return "User{" +
                    "userId='" + userId + '\'' +
                    ", access=" + access +
                    '}';
        }
    }
}
