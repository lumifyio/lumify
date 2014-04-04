package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;

import java.io.InputStream;
import java.util.Collection;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.*;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.*;

public class SecureGraphConcept extends Concept {
    private final Vertex vertex;

    public SecureGraphConcept(Vertex vertex) {
        this(vertex, null, null);
    }

    public SecureGraphConcept(Vertex vertex, String parentConceptIRI, Collection<OntologyProperty> properties) {
        super(parentConceptIRI, properties);
        this.vertex = vertex;
    }

    public String getTitle() {
        return ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public boolean hasGlyphIconResource() {
        // TODO: This can be changed to GLYPH_ICON.getPropertyValue(vertex) once ENTITY_IMAGE_URL is added
        return vertex.getPropertyValue(GLYPH_ICON.getKey()) != null;
    }

    public String getColor() {
        return COLOR.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return DISPLAY_NAME.getPropertyValue(vertex);
    }

    public String getDisplayType() {
        return DISPLAY_TYPE.getPropertyValue(vertex);
    }

    @Override
    public void setProperty(String name, Object value, Visibility visibility) {
        getVertex().setProperty(name, value, visibility);
    }

    @Override
    public InputStream getGlyphIcon() {
        StreamingPropertyValue spv = GLYPH_ICON.getPropertyValue(getVertex());
        if (spv == null) {
            return null;
        }
        return spv.getInputStream();
    }

    @Override
    public InputStream getMapGlyphIcon() {
        StreamingPropertyValue spv = MAP_GLYPH_ICON.getPropertyValue(getVertex());
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
