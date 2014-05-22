package io.lumify.securegraph.model.ontology;

import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyLumifyProperties;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.properties.LumifyProperties;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import org.securegraph.property.StreamingPropertyValue;

import java.io.InputStream;
import java.util.Collection;

public class SecureGraphConcept extends Concept {
    private final Vertex vertex;

    public SecureGraphConcept(Vertex vertex) {
        this(vertex, null, null);
    }

    public SecureGraphConcept(Vertex vertex, String parentConceptIRI, Collection<OntologyProperty> properties) {
        super(parentConceptIRI, properties);
        this.vertex = vertex;
    }

    @Override
    public String getTitle() {
        return OntologyLumifyProperties.ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    @Override
    public boolean hasGlyphIconResource() {
        // TODO: This can be changed to GLYPH_ICON.getPropertyValue(vertex) once ENTITY_IMAGE_URL is added
        return vertex.getPropertyValue(LumifyProperties.GLYPH_ICON.getKey()) != null;
    }

    @Override
    public String getColor() {
        return OntologyLumifyProperties.COLOR.getPropertyValue(vertex);
    }

    @Override
    public String getDisplayName() {
        return LumifyProperties.DISPLAY_NAME.getPropertyValue(vertex);
    }

    @Override
    public String getDisplayType() {
        return OntologyLumifyProperties.DISPLAY_TYPE.getPropertyValue(vertex);
    }

    @Override
    public String getTitleFormula() {
        return OntologyLumifyProperties.TITLE_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public String getSubtitleFormula() {
        return OntologyLumifyProperties.SUBTITLE_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public String getTimeFormula() {
        return OntologyLumifyProperties.TIME_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public boolean getUserVisible() {
        return OntologyLumifyProperties.USER_VISIBLE.getPropertyValue(vertex, true);
    }

    @Override
    public void setProperty(String name, Object value, Visibility visibility, Authorizations authorizations) {
        getVertex().setProperty(name, value, visibility, authorizations);
    }

    @Override
    public InputStream getGlyphIcon() {
        StreamingPropertyValue spv = LumifyProperties.GLYPH_ICON.getPropertyValue(getVertex());
        if (spv == null) {
            return null;
        }
        return spv.getInputStream();
    }

    @Override
    public InputStream getMapGlyphIcon() {
        StreamingPropertyValue spv = LumifyProperties.MAP_GLYPH_ICON.getPropertyValue(getVertex());
        if (spv == null) {
            return null;
        }
        return spv.getInputStream();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.vertex != null ? this.vertex.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SecureGraphConcept other = (SecureGraphConcept) obj;
        if (this.vertex != other.vertex && (this.vertex == null || !this.vertex.equals(other.vertex))) {
            return false;
        }
        return true;
    }

    public Vertex getVertex() {
        return this.vertex;
    }
}
