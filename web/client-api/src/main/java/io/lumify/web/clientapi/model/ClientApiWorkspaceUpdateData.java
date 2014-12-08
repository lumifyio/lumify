package io.lumify.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspaceUpdateData implements ClientApiObject {
    private String title;
    private List<EntityUpdate> entityUpdates = new ArrayList<EntityUpdate>();
    private List<String> entityDeletes = new ArrayList<String>();
    private List<UserUpdate> userUpdates = new ArrayList<UserUpdate>();
    private List<String> userDeletes = new ArrayList<String>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<EntityUpdate> getEntityUpdates() {
        return entityUpdates;
    }

    public List<String> getEntityDeletes() {
        return entityDeletes;
    }

    public List<String> getUserDeletes() {
        return userDeletes;
    }

    public List<UserUpdate> getUserUpdates() {
        return userUpdates;
    }

    public static class EntityUpdate {
        private String vertexId;
        private GraphPosition graphPosition;
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
    }

    public static class UserUpdate {
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
    }
}
