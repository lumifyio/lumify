package com.altamiracorp.lumify.core.ontology;

import com.altamiracorp.lumify.core.cmdline.OwlImport;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.model.IRI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BaseOntology {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BaseOntology.class);

    private final OntologyRepository ontologyRepository;
    private final Graph graph;
    private final OwlImport owlImport;

    @Inject
    public BaseOntology(OntologyRepository ontologyRepository, Graph graph, OwlImport owlImport) {
        this.ontologyRepository = ontologyRepository;
        this.graph = graph;
        this.owlImport = owlImport;
    }

    public void defineOntology(User user) {
        Concept rootConcept = ontologyRepository.getOrCreateConcept(null, OntologyRepository.ROOT_CONCEPT_IRI, "root");
        graph.flush();

        Concept entityConcept = ontologyRepository.getOrCreateConcept(rootConcept, OntologyRepository.ENTITY_CONCEPT_IRI, "thing");
        graph.flush();

        addEntityGlyphIcon(entityConcept);
        importBaseOwlFile();
    }

    private void importBaseOwlFile() {
        InputStream baseOwlFile = getClass().getResourceAsStream("/com/altamiracorp/lumify/core/ontology/base.owl");
        try {
            this.owlImport.importFile(baseOwlFile, IRI.create("http://lumify.io"));
        } catch (Exception e) {
            throw new LumifyException("Could not import ontology file", e);
        } finally {
            try {
                baseOwlFile.close();
            } catch (IOException ex) {
                throw new LumifyException("Could not close file", ex);
            }
        }
    }

    private void addEntityGlyphIcon(Concept entityConcept) {
        InputStream entityGlyphIconInputStream = this.getClass().getResourceAsStream("entity.png");

        try {
            ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
            IOUtils.copy(entityGlyphIconInputStream, imgOut);

            byte[] rawImg = imgOut.toByteArray();

            StreamingPropertyValue raw = new StreamingPropertyValue(new ByteArrayInputStream(rawImg), byte[].class);
            raw.searchIndex(false);
            LumifyProperties.GLYPH_ICON.setProperty(entityConcept.getVertex(), raw, OntologyRepository.VISIBILITY.getVisibility());
            graph.flush();
        } catch (IOException e) {
            throw new LumifyException("invalid stream for glyph icon");
        }
    }

    public boolean isOntologyDefined(User user) {
        try {
            Concept concept = ontologyRepository.getConceptById(OntologyRepository.ROOT_CONCEPT_IRI);
            return concept != null; // todo should check for more
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains(OntologyLumifyProperties.ONTOLOGY_TITLE.getKey())) {
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
