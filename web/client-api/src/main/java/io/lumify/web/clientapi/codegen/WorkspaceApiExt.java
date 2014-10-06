package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.LumifyClientApiException;
import io.lumify.web.clientapi.codegen.model.WorkspaceDiffItem;
import io.lumify.web.clientapi.codegen.model.WorkspaceUpdateData;
import io.lumify.web.clientapi.codegen.model.WorkspaceUserUpdate;
import io.lumify.web.clientapi.model.*;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceApiExt extends WorkspaceApi {
    public void update(WorkspaceUpdateData updateData) throws ApiException {
        update(ApiInvoker.serialize(updateData));
    }

    public WorkspacePublishResponse publishAll(List<WorkspaceDiffItem> diffItems) throws ApiException {
        List<PublishItem> publishItems = new ArrayList<PublishItem>();
        for (WorkspaceDiffItem diffItem : diffItems) {
            publishItems.add(workspaceDiffItemToPublishItem(diffItem));
        }
        return publish(publishItems);
    }

    public WorkspacePublishResponse publish(List<PublishItem> publishItems) throws ApiException {
        return publish(ApiInvoker.serialize(publishItems));
    }

    public PublishItem workspaceDiffItemToPublishItem(WorkspaceDiffItem workspaceDiffItem) {
        if ("VertexDiffItem".equals(workspaceDiffItem.getType())) {
            VertexPublishItem publishItem = new VertexPublishItem();
            publishItem.setAction(PublishItem.Action.addOrUpdate);
            publishItem.setVertexId(workspaceDiffItem.getVertexId());
            return publishItem;
        } else if ("PropertyDiffItem".equals(workspaceDiffItem.getType())) {
            PropertyPublishItem publishItem = new PropertyPublishItem();
            publishItem.setElementId(workspaceDiffItem.getElementId());
            publishItem.setKey(workspaceDiffItem.getKey());
            publishItem.setName(workspaceDiffItem.getName());
            return publishItem;
        } else if ("EdgeDiffItem".equals(workspaceDiffItem.getType())) {
            RelationshipPublishItem publishItem = new RelationshipPublishItem();
            publishItem.setEdgeId(workspaceDiffItem.getEdgeId());
            return publishItem;
        } else {
            throw new LumifyClientApiException("Unhandled WorkspaceDiffItem type: " + workspaceDiffItem.getType());
        }
    }

    public void setUserAccess(String userId, String access) throws ApiException {
        WorkspaceUpdateData addUser2WorkspaceUpdate = new WorkspaceUpdateData();
        WorkspaceUserUpdate addUser2Update = new WorkspaceUserUpdate();
        addUser2Update.setUserId(userId);
        addUser2Update.setAccess(access);
        addUser2WorkspaceUpdate.getUserUpdates().add(addUser2Update);
        update(addUser2WorkspaceUpdate);
    }
}
