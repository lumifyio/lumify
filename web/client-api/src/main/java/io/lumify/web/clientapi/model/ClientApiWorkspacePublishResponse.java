package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspacePublishResponse implements ClientApiObject {
    private List<ClientApiPublishItem> failures = new ArrayList<ClientApiPublishItem>();

    public List<ClientApiPublishItem> getFailures() {
        return failures;
    }

    public boolean isSuccess() {
        return failures.size() == 0;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    public void addFailure(ClientApiPublishItem data) {
        this.failures.add(data);
    }
}
