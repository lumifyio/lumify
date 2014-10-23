package io.lumify.analystsNotebook;

public enum AnalystsNotebookVersion {
    VERSION_6("6"),
    VERSION_7_OR_8("7 or 8");

    private String string;

    private AnalystsNotebookVersion(String string) {
        this.string = string;
    }

    public String toString() {
        return string;
    }
}
