package com.altamiracorp.lumify.core.ontology;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.ontology.PropertyType;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BaseOntology {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BaseOntology.class);

    private final OntologyRepository ontologyRepository;
    private final Graph graph;

    @Inject
    public BaseOntology(OntologyRepository ontologyRepository, Graph graph) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
    }

    public void defineOntology(User user) {
        // concept properties
        ontologyRepository.getOrCreatePropertyType(PropertyName.TITLE.toString(), PropertyType.STRING);
        ontologyRepository.getOrCreatePropertyType(PropertyName.GEO_LOCATION.toString(), PropertyType.GEO_LOCATION);
        ontologyRepository.getOrCreatePropertyType(PropertyName.GEO_LOCATION_DESCRIPTION.toString(), PropertyType.STRING);
        ontologyRepository.getOrCreatePropertyType(PropertyName.AUTHOR.toString(), PropertyType.STRING);
        graph.flush();

        Concept rootConcept = ontologyRepository.getOrCreateConcept(null, OntologyRepository.ROOT_CONCEPT_NAME, OntologyRepository.ROOT_CONCEPT_NAME);
        ontologyRepository.addPropertyTo(rootConcept.getVertex(), PropertyName.GLYPH_ICON.toString(), "glyph icon", PropertyType.IMAGE);
        ontologyRepository.addPropertyTo(rootConcept.getVertex(), PropertyName.MAP_GLYPH_ICON.toString(), "map glyph icon", PropertyType.IMAGE);
        graph.flush();

        // Entity concept
        Concept entity = ontologyRepository.getOrCreateConcept(rootConcept, OntologyRepository.TYPE_ENTITY.toString(), "Entity");
        ontologyRepository.addPropertyTo(entity.getVertex(), PropertyName.CONCEPT_TYPE.toString(), "Type", PropertyType.STRING);
        ontologyRepository.addPropertyTo(entity.getVertex(), PropertyName.TITLE.toString(), "Title", PropertyType.STRING);

        graph.flush();

        InputStream entityGlyphIconInputStream = this.getClass().getResourceAsStream("entity.png");

        try {
            ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
            IOUtils.copy(entityGlyphIconInputStream, imgOut);

            byte[] rawImg = imgOut.toByteArray();

            StreamingPropertyValue raw = new StreamingPropertyValue(new ByteArrayInputStream(rawImg), byte[].class);
            raw.searchIndex(false);
            entity.setProperty(PropertyName.GLYPH_ICON.toString(), raw, OntologyRepository.DEFAULT_VISIBILITY);
            graph.flush();
        } catch (IOException e) {
            throw new RuntimeException("invalid stream for glyph icon");
        }

    }

    public boolean isOntologyDefined(User user) {
        try {
            Concept concept = ontologyRepository.getConceptByName(OntologyRepository.TYPE_ENTITY);
            return concept != null; // todo should check for more
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains(PropertyName.ONTOLOGY_TITLE.toString())) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }

    public void initialize(User user) {
        if (!isOntologyDefined(user)) {
            LOGGER.info("Base ontology not defined. Creating a new ontology.");
            defineOntology(user);
        } else {
            LOGGER.info("Base ontology already defined.");
        }
    }
}
