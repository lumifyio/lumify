package io.lumify.analystsNotebook;

import java.util.EnumSet;

import static io.lumify.analystsNotebook.AnalystsNotebookFeature.*;

public enum AnalystsNotebookVersion {
    VERSION_6("6",
              "http://lumify.io/analystsNotebook/v6",
              "General",
              EnumSet.of(END_X,
                         LINK_STYLE_STRENGTH)
    ),

    VERSION_7("7",
              "http://lumify.io/analystsNotebook/v7",
              "General",
              EnumSet.of(SUMMARY,
                         PRINT_SETTINGS,
                         CHART_ITEM_X_POSITION)
    ),

    VERSION_8_5_1("8.5.1",
              "http://lumify.io/analystsNotebook/v8.5.1",
              "General",
              EnumSet.of(SUMMARY,
                         PRINT_SETTINGS,
                         CHART_ITEM_X_POSITION)
    ),

    VERSION_8_9("8.9",
              "http://lumify.io/analystsNotebook/v8.9",
              "General",
              EnumSet.of(SUMMARY,
                         PRINT_SETTINGS,
                         CHART_ITEM_X_POSITION,
                         CUSTOM_IMAGE_COLLECTION,
                         ICON_PICTURE)
    );

    private String string;
    private String ontologyConceptMetadataKeyPrefix;
    private String defaultIconFile;
    private EnumSet<AnalystsNotebookFeature> analystsNotebookFeatures;

    private AnalystsNotebookVersion(String string,
                                    String ontologyConceptMetadataKeyPrefix,
                                    String defaultIconFile,
                                    EnumSet<AnalystsNotebookFeature> analystsNotebookFeatures) {
        this.string = string;
        this.ontologyConceptMetadataKeyPrefix = ontologyConceptMetadataKeyPrefix;
        this.defaultIconFile = defaultIconFile;
        this.analystsNotebookFeatures = analystsNotebookFeatures;
    }

    @Override
    public String toString() {
        return string;
    }

    public String getOntologyConceptMetadataKeyPrefix() {
        return ontologyConceptMetadataKeyPrefix;
    }

    public String getDefaultIconFile() {
        return defaultIconFile;
    }

    public boolean supports(AnalystsNotebookFeature feature) {
        return analystsNotebookFeatures.contains(feature);
    }
}
