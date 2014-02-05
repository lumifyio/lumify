package com.altamiracorp.lumify.core.ontology;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.ONTOLOGY_TITLE;
import static com.altamiracorp.lumify.core.model.properties.EntityLumifyProperties.*;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.*;
import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.*;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyType;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

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
        ontologyRepository.getOrCreatePropertyType(TITLE.getKey(), PropertyType.STRING, "Title");
        ontologyRepository.getOrCreatePropertyType(GEO_LOCATION.getKey(), PropertyType.GEO_LOCATION, "Geo Location");
        ontologyRepository.getOrCreatePropertyType(GEO_LOCATION_DESCRIPTION.getKey(), PropertyType.STRING, "Geo Location Description");
        ontologyRepository.getOrCreatePropertyType(AUTHOR.getKey(), PropertyType.STRING, "Author");
        graph.flush();

        Concept rootConcept = ontologyRepository.getOrCreateConcept(null, OntologyRepository.ROOT_CONCEPT_NAME,
                OntologyRepository.ROOT_CONCEPT_NAME);
        ontologyRepository.addPropertyTo(rootConcept.getVertex(), GLYPH_ICON.getKey(), "glyph icon",
                PropertyType.IMAGE);
        ontologyRepository.addPropertyTo(rootConcept.getVertex(), MAP_GLYPH_ICON.getKey(), "map glyph icon",
                PropertyType.IMAGE);
        graph.flush();

        // Entity concept
        Concept entity = ontologyRepository.getOrCreateConcept(rootConcept, OntologyRepository.TYPE_ENTITY, "Entity");
        ontologyRepository.addPropertyTo(entity.getVertex(), CONCEPT_TYPE.getKey(), "Type", PropertyType.STRING);
        ontologyRepository.addPropertyTo(entity.getVertex(), TITLE.getKey(), "Title", PropertyType.STRING);

        graph.flush();

        InputStream entityGlyphIconInputStream = this.getClass().getResourceAsStream("entity.png");

        try {
            ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
            IOUtils.copy(entityGlyphIconInputStream, imgOut);

            byte[] rawImg = imgOut.toByteArray();

            StreamingPropertyValue raw = new StreamingPropertyValue(new ByteArrayInputStream(rawImg), byte[].class);
            raw.searchIndex(false);
            GLYPH_ICON.setProperty(entity.getVertex(), raw, OntologyRepository.DEFAULT_VISIBILITY);
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
            if (e.getMessage() != null && e.getMessage().contains(ONTOLOGY_TITLE.getKey())) {
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
