package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspaceUndoResponse implements ClientApiObject {
    private List<ClientApiUndoItem> failures = new ArrayList<ClientApiUndoItem>();

    public List<ClientApiUndoItem> getFailures() {
        return failures;
    }

    public boolean isSuccess() {
        return failures.size() == 0;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    public void addFailure(ClientApiUndoItem data) {
        this.failures.add(data);
    }
}
