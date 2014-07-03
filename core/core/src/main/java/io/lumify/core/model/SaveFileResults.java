package io.lumify.core.model;

public class SaveFileResults {
    private final String rowKey;
    private final String fullPath;

    public SaveFileResults(String rowKey, String fullPath) {
        this.rowKey = rowKey;
        this.fullPath = fullPath;
    }

    public String getRowKey() {
        return rowKey;
    }

    public String getFullPath() {
        return fullPath;
    }
}
