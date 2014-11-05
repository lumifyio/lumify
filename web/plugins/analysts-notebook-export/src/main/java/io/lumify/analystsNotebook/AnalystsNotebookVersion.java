package io.lumify.analystsNotebook;

public enum AnalystsNotebookVersion {
    VERSION_6("6", "http://lumify.io/analystsNotebook/version6"),
    VERSION_7_OR_8("7 or 8", "http://lumify.io/analystsNotebook/version7or8");

    private String string;
    private String ontologyConceptMetadataIconFileKey;

    private AnalystsNotebookVersion(String string, String ontologyConceptMetadataIconFileKey) {
        this.string = string;
        this.ontologyConceptMetadataIconFileKey = ontologyConceptMetadataIconFileKey;
    }

    public String toString() {
        return string;
    }

    public String getOntologyConceptMetadataIconFileKey() {
        return ontologyConceptMetadataIconFileKey;
    }
}
