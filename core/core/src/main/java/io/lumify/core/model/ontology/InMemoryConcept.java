package io.lumify.core.model.ontology;

import io.lumify.core.exception.LumifyException;
import org.securegraph.Authorizations;

import java.util.ArrayList;

public class InMemoryConcept extends Concept {
    private String title;
    private String color;
    private String displayName;
    private String displayType;
    private String titleFormula;
    private String subtitleFormula;
    private String timeFormula;
    private boolean glyphIconResource;
    private String conceptIRI;
    private byte[] glyphIcon;
    private boolean userVisible;

    protected InMemoryConcept(String conceptIRI, String parentIRI) {
        super(parentIRI, new ArrayList<OntologyProperty>());
        this.conceptIRI = conceptIRI;
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
        if (OntologyLumifyProperties.COLOR.getPropertyName().equals(name)) {
            this.color = (String) value;
        } else if (OntologyLumifyProperties.DISPLAY_TYPE.getPropertyName().equals(name)) {
            this.displayType = (String) value;
        } else if (OntologyLumifyProperties.TITLE_FORMULA.getPropertyName().equals(name)) {
            this.titleFormula = (String) value;
        } else if (OntologyLumifyProperties.SUBTITLE_FORMULA.getPropertyName().equals(name)) {
            this.subtitleFormula = (String) value;
        } else if (OntologyLumifyProperties.TIME_FORMULA.getPropertyName().equals(name)) {
            this.timeFormula = (String) value;
        } else if (OntologyLumifyProperties.USER_VISIBLE.getPropertyName().equals(name)) {
            this.userVisible = (Boolean) value;
        } else {
            throw new LumifyException("Set not implemented for property " + name);
        }
    }

    @Override
    public void removeProperty(String name, Authorizations authorizations) {
        if (OntologyLumifyProperties.TITLE_FORMULA.getPropertyName().equals(name)) {
            this.titleFormula = null;
        } else if (OntologyLumifyProperties.SUBTITLE_FORMULA.getPropertyName().equals(name)) {
            this.subtitleFormula = null;
        } else if (OntologyLumifyProperties.TIME_FORMULA.getPropertyName().equals(name)) {
            this.timeFormula = null;
        } else {
            throw new LumifyException("Remove not implemented for property " + name);
        }
    }

    @Override
    public byte[] getGlyphIcon() {
        return glyphIcon;
    }

    public void setHasGlyphIcon(boolean hasGlyphIcon) {
        glyphIconResource = hasGlyphIcon;
    }

    public void setGlyphIcon(byte[] inputStream) {
        this.glyphIcon = inputStream;
    }

    @Override
    public byte[] getMapGlyphIcon() {
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
