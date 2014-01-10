package com.altamiracorp.lumify.core.ontology;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.ontology.PropertyType;
import com.altamiracorp.lumify.core.model.resources.ResourceRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;

import java.io.InputStream;

public class BaseOntology {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BaseOntology.class);
    private static final Visibility DEFAULT_VISIBILITY = new Visibility("");

    private final OntologyRepository ontologyRepository;
    private final ResourceRepository resourceRepository;
    private final Graph graph;

    @Inject
    public BaseOntology(OntologyRepository ontologyRepository, ResourceRepository resourceRepository, Graph graph) {
        this.ontologyRepository = ontologyRepository;
        this.resourceRepository = resourceRepository;
        this.graph = graph;
    }

    public void defineOntology(User user) {
        // concept properties
        ontologyRepository.getOrCreatePropertyType(PropertyName.TITLE.toString(), PropertyType.STRING, user);
        ontologyRepository.getOrCreatePropertyType(PropertyName.GEO_LOCATION.toString(), PropertyType.GEO_LOCATION, user);
        ontologyRepository.getOrCreatePropertyType(PropertyName.GEO_LOCATION_DESCRIPTION.toString(), PropertyType.STRING, user);
        ontologyRepository.getOrCreatePropertyType(PropertyName.AUTHOR.toString(), PropertyType.STRING, user);
        graph.flush();

        Concept rootConcept = ontologyRepository.getOrCreateConcept(null, OntologyRepository.ROOT_CONCEPT_NAME, OntologyRepository.ROOT_CONCEPT_NAME, user);
        ontologyRepository.addPropertyTo(rootConcept.getVertex(), PropertyName.GLYPH_ICON.toString(), "glyph icon", PropertyType.IMAGE, user);
        ontologyRepository.addPropertyTo(rootConcept.getVertex(), PropertyName.MAP_GLYPH_ICON.toString(), "map glyph icon", PropertyType.IMAGE, user);
        graph.flush();

        // Entity concept
        Concept entity = ontologyRepository.getOrCreateConcept(rootConcept, OntologyRepository.TYPE_ENTITY.toString(), "Entity", user);
        ontologyRepository.addPropertyTo(entity.getVertex(), PropertyName.CONCEPT_TYPE.toString(), "Type", PropertyType.STRING, user);
        ontologyRepository.addPropertyTo(entity.getVertex(), PropertyName.TITLE.toString(), "Title", PropertyType.STRING, user);

        graph.flush();

        InputStream entityGlyphIconInputStream = this.getClass().getResourceAsStream("entity.png");
        String entityGlyphIconRowKey = resourceRepository.importFile(entityGlyphIconInputStream, "png", user);
        entity.setProperty(PropertyName.GLYPH_ICON.toString(), entityGlyphIconRowKey, DEFAULT_VISIBILITY);
        graph.flush();
    }

    public boolean isOntologyDefined(User user) {
        try {
            Concept concept = ontologyRepository.getConceptByName(OntologyRepository.TYPE_ENTITY, user);
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
