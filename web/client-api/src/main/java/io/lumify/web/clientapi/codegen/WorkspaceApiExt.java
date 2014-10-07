package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.LumifyClientApiException;
import io.lumify.web.clientapi.model.*;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceApiExt extends WorkspaceApi {
    public void update(WorkspaceUpdateData updateData) throws ApiException {
        update(ApiInvoker.serialize(updateData));
    }

    public WorkspacePublishResponse publishAll(List<WorkspaceDiff.Item> diffItems) throws ApiException {
        List<PublishItem> publishItems = new ArrayList<PublishItem>();
        for (WorkspaceDiff.Item diffItem : diffItems) {
            publishItems.add(workspaceDiffItemToPublishItem(diffItem));
        }
        return publish(publishItems);
    }

    public WorkspacePublishResponse publish(List<PublishItem> publishItems) throws ApiException {
        return publish(ApiInvoker.serialize(publishItems));
    }

    public PublishItem workspaceDiffItemToPublishItem(WorkspaceDiff.Item workspaceDiffItem) {
        if (workspaceDiffItem instanceof WorkspaceDiff.VertexItem) {
            WorkspaceDiff.VertexItem vertexDiffItem = (WorkspaceDiff.VertexItem) workspaceDiffItem;
            VertexPublishItem publishItem = new VertexPublishItem();
            publishItem.setAction(PublishItem.Action.addOrUpdate);
            publishItem.setVertexId(vertexDiffItem.getVertexId());
            return publishItem;
        } else if (workspaceDiffItem instanceof WorkspaceDiff.PropertyItem) {
            WorkspaceDiff.PropertyItem propertyDiffItem = (WorkspaceDiff.PropertyItem) workspaceDiffItem;
            PropertyPublishItem publishItem = new PropertyPublishItem();
            publishItem.setElementId(propertyDiffItem.getElementId());
            publishItem.setKey(propertyDiffItem.getKey());
            publishItem.setName(propertyDiffItem.getName());
            return publishItem;
        } else if (workspaceDiffItem instanceof WorkspaceDiff.EdgeItem) {
            WorkspaceDiff.EdgeItem edgeDiffItem = (WorkspaceDiff.EdgeItem) workspaceDiffItem;
            RelationshipPublishItem publishItem = new RelationshipPublishItem();
            publishItem.setEdgeId(edgeDiffItem.getEdgeId());
            return publishItem;
        } else {
            throw new LumifyClientApiException("Unhandled WorkspaceDiffItem type: " + workspaceDiffItem.getType());
        }
    }

    public void setUserAccess(String userId, WorkspaceAccess access) throws ApiException {
        WorkspaceUpdateData addUser2WorkspaceUpdate = new WorkspaceUpdateData();
        WorkspaceUpdateData.UserUpdate addUser2Update = new WorkspaceUpdateData.UserUpdate();
        addUser2Update.setUserId(userId);
        addUser2Update.setAccess(access);
        addUser2WorkspaceUpdate.getUserUpdates().add(addUser2Update);
        update(addUser2WorkspaceUpdate);
    }
}
