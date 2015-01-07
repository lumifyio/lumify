package io.lumify.web.clientapi;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.*;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceApiExt extends io.lumify.web.clientapi.codegen.WorkspaceApi {
    public void update(ClientApiWorkspaceUpdateData updateData) throws ApiException {
        update(ApiInvoker.serialize(updateData));
    }

    public ClientApiWorkspacePublishResponse publishAll(List<ClientApiWorkspaceDiff.Item> diffItems) throws ApiException {
        List<ClientApiPublishItem> publishItems = new ArrayList<ClientApiPublishItem>();
        for (ClientApiWorkspaceDiff.Item diffItem : diffItems) {
            publishItems.add(workspaceDiffItemToPublishItem(diffItem));
        }
        return publish(publishItems);
    }

    public ClientApiWorkspacePublishResponse publish(List<ClientApiPublishItem> publishItems) throws ApiException {
        return publish(ApiInvoker.serialize(publishItems));
    }

    public ClientApiPublishItem workspaceDiffItemToPublishItem(ClientApiWorkspaceDiff.Item workspaceDiffItem) {
        if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.VertexItem) {
            ClientApiWorkspaceDiff.VertexItem vertexDiffItem = (ClientApiWorkspaceDiff.VertexItem) workspaceDiffItem;
            ClientApiVertexPublishItem publishItem = new ClientApiVertexPublishItem();
            publishItem.setAction(ClientApiPublishItem.Action.addOrUpdate);
            publishItem.setVertexId(vertexDiffItem.getVertexId());
            return publishItem;
        } else if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.PropertyItem) {
            ClientApiWorkspaceDiff.PropertyItem propertyDiffItem = (ClientApiWorkspaceDiff.PropertyItem) workspaceDiffItem;
            ClientApiPropertyPublishItem publishItem = new ClientApiPropertyPublishItem();
            publishItem.setElementId(propertyDiffItem.getElementId());
            publishItem.setKey(propertyDiffItem.getKey());
            publishItem.setName(propertyDiffItem.getName());
            publishItem.setVisibilityString (propertyDiffItem.getVisibilityString());
            return publishItem;
        } else if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.EdgeItem) {
            ClientApiWorkspaceDiff.EdgeItem edgeDiffItem = (ClientApiWorkspaceDiff.EdgeItem) workspaceDiffItem;
            ClientApiRelationshipPublishItem publishItem = new ClientApiRelationshipPublishItem();
            publishItem.setEdgeId(edgeDiffItem.getEdgeId());
            return publishItem;
        } else {
            throw new LumifyClientApiException("Unhandled WorkspaceDiffItem type: " + workspaceDiffItem.getType());
        }
    }

    public ClientApiWorkspaceUndoResponse undoAll(List<ClientApiWorkspaceDiff.Item> diffItems) throws ApiException {
        List<ClientApiUndoItem> undoItems = new ArrayList<ClientApiUndoItem>();
        for (ClientApiWorkspaceDiff.Item diffItem : diffItems) {
            undoItems.add(workspaceDiffItemToUndoItem(diffItem));
        }
        return undo(undoItems);
    }

    public ClientApiWorkspaceUndoResponse undo(List<ClientApiUndoItem> undoItems) throws ApiException {
        return undo(ApiInvoker.serialize(undoItems));
    }

    public ClientApiUndoItem workspaceDiffItemToUndoItem(ClientApiWorkspaceDiff.Item workspaceDiffItem) {
        if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.VertexItem) {
            ClientApiWorkspaceDiff.VertexItem vertexDiffItem = (ClientApiWorkspaceDiff.VertexItem) workspaceDiffItem;
            ClientApiVertexUndoItem undoItem = new ClientApiVertexUndoItem();
            undoItem.setVertexId(vertexDiffItem.getVertexId());
            return undoItem;
        } else if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.PropertyItem) {
            ClientApiWorkspaceDiff.PropertyItem propertyDiffItem = (ClientApiWorkspaceDiff.PropertyItem) workspaceDiffItem;
            ClientApiPropertyUndoItem undoItem = new ClientApiPropertyUndoItem();
            undoItem.setElementId(propertyDiffItem.getElementId());
            undoItem.setKey(propertyDiffItem.getKey());
            undoItem.setName(propertyDiffItem.getName());
            undoItem.setVisibilityString(propertyDiffItem.getVisibilityString());
            return undoItem;
        } else if (workspaceDiffItem instanceof ClientApiWorkspaceDiff.EdgeItem) {
            ClientApiWorkspaceDiff.EdgeItem edgeDiffItem = (ClientApiWorkspaceDiff.EdgeItem) workspaceDiffItem;
            ClientApiRelationshipUndoItem undoItem = new ClientApiRelationshipUndoItem();
            undoItem.setEdgeId(edgeDiffItem.getEdgeId());
            return undoItem;
        } else {
            throw new LumifyClientApiException("Unhandled WorkspaceDiffItem type: " + workspaceDiffItem.getType());
        }
    }

    public void setUserAccess(String userId, WorkspaceAccess access) throws ApiException {
        ClientApiWorkspaceUpdateData addUser2WorkspaceUpdate = new ClientApiWorkspaceUpdateData();
        ClientApiWorkspaceUpdateData.UserUpdate addUser2Update = new ClientApiWorkspaceUpdateData.UserUpdate();
        addUser2Update.setUserId(userId);
        addUser2Update.setAccess(access);
        addUser2WorkspaceUpdate.getUserUpdates().add(addUser2Update);
        update(addUser2WorkspaceUpdate);
    }
}
