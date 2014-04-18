package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.securegraph.Visibility;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

public class InMemoryConcept extends Concept {
    private String title;
    private String color;
    private String displayName;
    private String displayType;
    private ArrayList<OntologyProperty> properties;
    private boolean glyphIconResource;
    private String conceptIRI;
    private InputStream glyphIconInputStream;

    protected InMemoryConcept(String conceptIRI, String parentIRI, Collection<OntologyProperty> properties) {
        super(parentIRI, properties);
        this.conceptIRI = conceptIRI;
        this.properties = (ArrayList<OntologyProperty>) properties;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean hasGlyphIconResource() {
        return glyphIconResource;
    }

    @Override
    public String getColor() {
        return color;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDisplayType() {
        return displayType;
    }

    @Override
    public void setProperty(String name, Object value, Visibility visibility) {
        return;
    }

    @Override
    public InputStream getGlyphIcon() {
        return null;
    }

    public void setGlyphIconInputStream (InputStream inputStream) {
        this.glyphIconInputStream = inputStream;
    }

    @Override
    public InputStream getMapGlyphIcon() {
        return null;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    public ArrayList<OntologyProperty> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<OntologyProperty> properties) {
        this.properties = properties;
    }

    public String getConceptIRI() {
        return conceptIRI;
    }

    public void setConceptIRI(String conceptIRI) {
        this.conceptIRI = conceptIRI;
    }
}
