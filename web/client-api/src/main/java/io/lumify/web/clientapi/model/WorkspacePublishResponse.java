package io.lumify.web.clientapi.model;

import com.google.common.base.Joiner;

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
        return "WorkspacePublishResponse{" +
                "failures=" + Joiner.on(",").join(failures) +
                '}';
    }

    public void addFailure(PublishItem data) {
        this.failures.add(data);
    }
}
