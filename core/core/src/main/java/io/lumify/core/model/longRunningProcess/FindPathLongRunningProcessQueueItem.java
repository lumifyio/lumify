package io.lumify.core.model.longRunningProcess;

import io.lumify.core.util.ClientApiConverter;
import org.json.JSONObject;
import org.securegraph.Authorizations;

public class FindPathLongRunningProcessQueueItem {
    private String sourceVertexId;
    private String destVertexId;
    private int hops;
    private String workspaceId;
    private String[] authorizations;

    public FindPathLongRunningProcessQueueItem() {

    }

    public FindPathLongRunningProcessQueueItem(String sourceVertexId, String destVertexId, int hops, String workspaceId, Authorizations authorizations) {
        this.sourceVertexId = sourceVertexId;
        this.destVertexId = destVertexId;
        this.hops = hops;
        this.workspaceId = workspaceId;
        this.authorizations = authorizations.getAuthorizations();
    }

    public String getSourceVertexId() {
        return sourceVertexId;
    }

    public String getDestVertexId() {
        return destVertexId;
    }

    public int getHops() {
        return hops;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String[] getAuthorizations() {
        return authorizations;
    }

    public String getType() {
        return "findPath";
    }

    public JSONObject toJson() {
        return new JSONObject(ClientApiConverter.clientApiToString(this));
    }
}
