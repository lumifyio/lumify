package io.lumify.core.model.ontology;

import org.securegraph.Authorizations;
import org.securegraph.Visibility;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

public class InMemoryConcept extends Concept {
    private String title;
    private String color;
    private String displayName;
    private String displayType;
    private String titleFormula;
    private String subtitleFormula;
    private String timeFormula;
    private ArrayList<OntologyProperty> properties;
    private boolean glyphIconResource;
    private String conceptIRI;
    private InputStream glyphIconInputStream;
    private boolean userVisible;

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
    public String getTitleFormula() {
        return titleFormula;
    }

    @Override
    public String getSubtitleFormula() {
        return this.subtitleFormula;
    }

    @Override
    public String getTimeFormula() {
        return this.timeFormula;
    }

    @Override
    public boolean getUserVisible() {
        return userVisible;
    }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
    }

    @Override
    public void removeProperty(String name, Authorizations authorizations) {
    }

    @Override
    public InputStream getGlyphIcon() {
        return glyphIconInputStream;
    }

    public void setHasGlyphIcon(boolean hasGlyphIcon) {
        glyphIconResource = hasGlyphIcon;
    }

    public void setGlyphIconInputStream(InputStream inputStream) {
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

    public void setTitleFormula(String titleFormula) {
        this.titleFormula = titleFormula;
    }

    public void setUserVisible(boolean userVisible) {
        this.userVisible = userVisible;
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

    public void setSubtitleFormula(String subtitleFormula) {
        this.subtitleFormula = subtitleFormula;
    }

    public void setTimeFormula(String timeFormula) {
        this.timeFormula = timeFormula;
    }
}
