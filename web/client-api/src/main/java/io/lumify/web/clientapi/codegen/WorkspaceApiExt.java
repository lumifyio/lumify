package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.LumifyClientApiException;
import io.lumify.web.clientapi.codegen.model.PublishItem;
import io.lumify.web.clientapi.codegen.model.PublishResponse;
import io.lumify.web.clientapi.codegen.model.WorkspaceDiffItem;
import io.lumify.web.clientapi.codegen.model.WorkspaceUpdateData;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceApiExt extends WorkspaceApi {
    public void update(WorkspaceUpdateData updateData) throws ApiException {
        update(ApiInvoker.serialize(updateData));
    }

    public PublishResponse publishAll(List<WorkspaceDiffItem> diffItems) throws ApiException {
        List<PublishItem> publishItems = new ArrayList<PublishItem>();
        for (WorkspaceDiffItem diffItem : diffItems) {
            publishItems.add(workspaceDiffItemToPublishItem(diffItem));
        }
        return publish(publishItems);
    }

    public PublishResponse publish(List<PublishItem> publishItems) throws ApiException {
        return publish(ApiInvoker.serialize(publishItems));
    }

    public PublishItem workspaceDiffItemToPublishItem(WorkspaceDiffItem workspaceDiffItem) {
        PublishItem publishItem = new PublishItem();
        publishItem.setAction("");
        if ("VertexDiffItem".equals(workspaceDiffItem.getType())) {
            publishItem.setType("vertex");
            publishItem.setVertexId(workspaceDiffItem.getVertexId());
        } else if ("PropertyDiffItem".equals(workspaceDiffItem.getType())) {
            publishItem.setType("property");
            publishItem.setVertexId(workspaceDiffItem.getElementId());
            publishItem.setKey(workspaceDiffItem.getKey());
            publishItem.setName(workspaceDiffItem.getName());
        } else if ("EdgeDiffItem".equals(workspaceDiffItem.getType())) {
            publishItem.setType("relationship");
            publishItem.setEdgeId(workspaceDiffItem.getEdgeId());
        } else {
            throw new LumifyClientApiException("Unhandled WorkspaceDiffItem type: " + workspaceDiffItem.getType());
        }
        return publishItem;
    }
}
