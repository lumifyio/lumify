package io.lumify.core.ingest.graphProperty;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.securegraph.*;

import java.io.File;

public class GraphPropertyWorkData {
    private final VisibilityTranslator visibilityTranslator;
    private final Element element;
    private final Property property;
    private final String workspaceId;
    private final String visibilitySource;
    private File localFile;

    public GraphPropertyWorkData(VisibilityTranslator visibilityTranslator, Element element, Property property, String workspaceId, String visibilitySource) {
        this.visibilityTranslator = visibilityTranslator;
        this.element = element;
        this.property = property;
        this.workspaceId = workspaceId;
        this.visibilitySource = visibilitySource;
    }

    public Element getElement() {
        return element;
    }

    public Property getProperty() {
        return property;
    }

    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public File getLocalFile() {
        return localFile;
    }

    public Visibility getVisibility() {
        return getElement().getVisibility();
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getVisibilitySource() {
        return visibilitySource;
    }

    // TODO this is a weird method. I'm not sure what this should be used for
    public VisibilityJson getVisibilitySourceJson() {
        if (getVisibilitySource() == null || getVisibilitySource().length() == 0) {
            return new VisibilityJson();
        }
        VisibilityJson visibilityJson = new VisibilityJson();
        visibilityJson.setSource(getVisibilitySource());
        return visibilityJson;
    }

    public VisibilityJson getVisibilityJson() {
        VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(getElement());
        if (visibilityJson != null) {
            return visibilityJson;
        }

        return getVisibilitySourceJson();
    }

    public Metadata createPropertyMetadata() {
        Metadata metadata = new Metadata();
        VisibilityJson visibilityJson = getVisibilityJson();
        if (visibilityJson != null) {
            LumifyProperties.VISIBILITY_JSON.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        }
        return metadata;
    }

    public void setVisibilityJsonOnElement(ElementBuilder builder) {
        VisibilityJson visibilityJson = getVisibilityJson();
        if (visibilityJson != null) {
            LumifyProperties.VISIBILITY_JSON.setProperty(builder, visibilityJson, visibilityTranslator.getDefaultVisibility());
        }
    }

    public void setVisibilityJsonOnElement(Element element, Authorizations authorizations) {
        VisibilityJson visibilityJson = getVisibilitySourceJson();
        if (visibilityJson != null) {
            LumifyProperties.VISIBILITY_JSON.setProperty(element, visibilityJson, visibilityTranslator.getDefaultVisibility(), authorizations);
        }
    }
}
