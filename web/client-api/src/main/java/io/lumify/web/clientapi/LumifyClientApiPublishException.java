package io.lumify.web.clientapi;

import io.lumify.web.clientapi.model.WorkspacePublishResponse;

public class LumifyClientApiPublishException extends LumifyClientApiException {
    private final WorkspacePublishResponse response;

    public LumifyClientApiPublishException(WorkspacePublishResponse response) {
        super("Failed to publish");
        this.response = response;
    }

    public WorkspacePublishResponse getResponse() {
        return response;
    }
}
