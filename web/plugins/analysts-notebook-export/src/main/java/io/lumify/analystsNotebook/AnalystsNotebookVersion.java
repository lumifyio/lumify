package io.lumify.analystsNotebook;

public enum AnalystsNotebookVersion {
    VERSION_6("6", "http://lumify.io/analystsNotebook/version6", "General"),
    VERSION_7_OR_8("7 or 8", "http://lumify.io/analystsNotebook/version7or8", "General");

    private String string;
    private String ontologyConceptMetadataIconFileKey;
    private String defaultIconFile;

    private AnalystsNotebookVersion(String string, String ontologyConceptMetadataIconFileKey, String defaultIconFile) {
        this.string = string;
        this.ontologyConceptMetadataIconFileKey = ontologyConceptMetadataIconFileKey;
        this.defaultIconFile = defaultIconFile;
    }

    @Override
    public String toString() {
        return string;
    }

    public String getOntologyConceptMetadataIconFileKey() {
        return ontologyConceptMetadataIconFileKey;
    }

    public String getDefaultIconFile() {
        return defaultIconFile;
    }
}
