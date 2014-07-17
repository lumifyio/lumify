package io.lumify.securegraph.model.ontology;

import io.lumify.core.exception.LumifyResourceNotFoundException;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyProperty;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import org.codehaus.plexus.util.IOUtil;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;
import org.securegraph.property.StreamingPropertyValue;

import java.io.IOException;
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
    public String getIRI() {
        return LumifyProperties.ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    @Override
    public String getTitle() {
        return LumifyProperties.ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    @Override
    public boolean hasGlyphIconResource() {
        // TODO: This can be changed to GLYPH_ICON.getPropertyValue(vertex) once ENTITY_IMAGE_URL is added
        return vertex.getPropertyValue(LumifyProperties.GLYPH_ICON.getPropertyName()) != null;
    }

    @Override
    public String getColor() {
        return LumifyProperties.COLOR.getPropertyValue(vertex);
    }

    @Override
    public String getDisplayName() {
        return LumifyProperties.DISPLAY_NAME.getPropertyValue(vertex);
    }

    @Override
    public String getDisplayType() {
        return LumifyProperties.DISPLAY_TYPE.getPropertyValue(vertex);
    }

    @Override
    public String getTitleFormula() {
        return LumifyProperties.TITLE_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public String getSubtitleFormula() {
        return LumifyProperties.SUBTITLE_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public String getTimeFormula() {
        return LumifyProperties.TIME_FORMULA.getPropertyValue(vertex);
    }

    @Override
    public boolean getUserVisible() {
        return LumifyProperties.USER_VISIBLE.getPropertyValue(vertex, true);
    }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
        getVertex().setProperty(name, value, OntologyRepository.VISIBILITY.getVisibility(), authorizations);
    }

    @Override
    public void removeProperty(String name, Authorizations authorizations) {
        getVertex().removeProperty(name, authorizations);
    }

    @Override
    public byte[] getGlyphIcon() {
        try {
            StreamingPropertyValue spv = LumifyProperties.GLYPH_ICON.getPropertyValue(getVertex());
            if (spv == null) {
                return null;
            }
            return IOUtil.toByteArray(spv.getInputStream());
        } catch (IOException e) {
            throw new LumifyResourceNotFoundException("Could not retrieve glyph icon");
        }
    }

    @Override
    public byte[] getMapGlyphIcon() {
        try {
            StreamingPropertyValue spv = LumifyProperties.MAP_GLYPH_ICON.getPropertyValue(getVertex());
            if (spv == null) {
                return null;
            }
            return IOUtil.toByteArray(spv.getInputStream());
        } catch (IOException e) {
            throw new LumifyResourceNotFoundException("Could not retrieve map glyph icon");
        }
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
