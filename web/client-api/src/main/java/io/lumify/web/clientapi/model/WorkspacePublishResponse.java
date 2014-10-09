package io.lumify.web.clientapi.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class WorkspacePublishResponse {
    private List<PublishItem> failures = new ArrayList<PublishItem>();

    public List<PublishItem> getFailures() {
        return failures;
    }

    public boolean isSuccess() {
        return failures.size() == 0;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    public void addFailure(PublishItem data) {
        this.failures.add(data);
    }
}
