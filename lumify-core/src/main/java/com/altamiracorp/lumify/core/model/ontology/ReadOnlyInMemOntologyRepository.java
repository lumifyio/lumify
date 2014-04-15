package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.exception.LumifyResourceNotFoundException;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class ReadOnlyInMemOntologyRepository extends OntologyRepositoryBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ReadOnlyInMemOntologyRepository.class);
    private Cache<String, InMemConcept> conceptsCache = CacheBuilder.newBuilder()
            .build();
    private Cache<String, InMemOntologyProperty> propertiesCache = CacheBuilder.newBuilder()
            .build();
    private Cache<String, InMemRelationship> relationshipsCache = CacheBuilder.newBuilder()
            .build();
    private Cache<String, byte[]> fileCache = CacheBuilder.newBuilder()
            .build();
    private OWLOntologyLoaderConfiguration owlConfig;

    public void init (Configuration config) throws Exception {
        Map<String, String> ontologies = config.getSubset(Configuration.ONTOLOGY_REPOSITORY_OWL);
        owlConfig = new OWLOntologyLoaderConfiguration();
        owlConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        if (!isOntologyDefined()) {
            LOGGER.info("Base ontology not defined. Creating a new ontology.");
            defineOntology();
        } else {
            LOGGER.info("Base ontology already defined.");
        }

        for (int i = 1; i < 1000; i ++) {
            String iri = ontologies.get(i + ".iri");
            String dir = ontologies.get(i + ".dir");
            String file = ontologies.get(i + ".file");

            if (iri != null) {
                if (dir != null) {
                    importFile(findOwlFile(new File(dir)), IRI.create(iri));
                } else if (file != null) {
                    writePackage(new File(file), IRI.create(iri));
                } else {
                    throw new LumifyResourceNotFoundException("iri without file or dir");
                }
            } else {
                break;
            }
        }
    }

    @Override
    protected Concept importOntologyClass(OWLOntology o, OWLClass ontologyClass, File inDir) throws IOException {
        String uri = ontologyClass.getIRI().toString();

        InMemConcept parent = (InMemConcept) getParentConcept(o, ontologyClass, inDir);
        InMemConcept result = (InMemConcept) getOrCreateConcept(parent, uri, getLabel(o, ontologyClass));

        String color = getColor(o, ontologyClass);
        if (color != null) {
            result.setColor(color);
        }

        String displayType = getDisplayType(o, ontologyClass);
        if (displayType != null) {
            result.setDisplayType(displayType);
        }

        String glyphIconFileName = getGlyphIconFileName(o, ontologyClass);
        if (glyphIconFileName != null) {
            File iconFile = new File(inDir, glyphIconFileName);
            if (!iconFile.exists()) {
                throw new RuntimeException("Could not find icon file: " + iconFile.toString());
            }
            InputStream iconFileIn = new FileInputStream(iconFile);
            try {
                result.setGlyphIconInputStream(iconFileIn);
            } finally {
                iconFileIn.close();
            }
        }
        conceptsCache.put(uri, result);
        return result;
    }

    @Override
    public void addEntityGlyphIcon(Concept entityConcept) {

    }

    @Override
    protected void storeOntologyFile(InputStream inputStream, IRI documentIRI) {
        try {
            byte[] inFileData = IOUtils.toByteArray(inputStream);
            fileCache.put(documentIRI.toString(), inFileData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected List<OWLOntology> loadOntologyFiles(OWLOntologyManager m, OWLOntologyLoaderConfiguration config, IRI excludedIRI) throws Exception {
        List<OWLOntology> loadedOntologies = new ArrayList<OWLOntology>();
        ConcurrentMap<String, byte[]> concurrentMap = fileCache.asMap();
        for (String key : concurrentMap.keySet()) {
            IRI lumifyBaseOntologyIRI = IRI.create(key);
            if (excludedIRI != null && excludedIRI.equals(lumifyBaseOntologyIRI)) {
                continue;
            }
            InputStream lumifyBaseOntologyIn = new ByteArrayInputStream(concurrentMap.get(key));
            try {
                Reader lumifyBaseOntologyReader = new InputStreamReader(lumifyBaseOntologyIn);
                LOGGER.info("Loading existing ontology: %s", key);
                OWLOntologyDocumentSource lumifyBaseOntologySource = new ReaderDocumentSource(lumifyBaseOntologyReader, lumifyBaseOntologyIRI);
                OWLOntology o = m.loadOntologyFromOntologyDocument(lumifyBaseOntologySource, config);
                loadedOntologies.add(o);
            } finally {
                lumifyBaseOntologyIn.close();
            }
        }
        return loadedOntologies;
    }

    @Override
    protected OntologyProperty addPropertyTo(Concept concept, String propertyIRI, String displayName, PropertyType dataType, boolean userVisible) {
        checkNotNull(concept, "concept was null");
        InMemOntologyProperty property = getOrCreatePropertyType(propertyIRI, dataType, displayName, userVisible);
        checkNotNull(property, "Could not find property: " + propertyIRI);
        return property;
    }

    private InMemOntologyProperty getOrCreatePropertyType(final String propertyName, final PropertyType dataType, final String displayName, boolean userVisible) {
        InMemOntologyProperty property = (InMemOntologyProperty) getProperty(propertyName);
        if (property == null) {
            property = new InMemOntologyProperty();
            property.setDataType(dataType);
            property.setUserVisible(userVisible);
            if (displayName != null && !displayName.trim().isEmpty()) {
                property.setDisplayName(displayName);
            }
            propertiesCache.put(propertyName, property);
        }
        return property;
    }

    @Override
    public void clearCache() {
    }

    @Override
    public Iterable<Relationship> getRelationshipLabels() {
        return new ConvertingIterable<InMemRelationship, Relationship>(relationshipsCache.asMap().values()) {
            @Override
            protected Relationship convert(InMemRelationship InMemRelationship) {
                return InMemRelationship;
            }
        };
    }

    @Override
    public Iterable<OntologyProperty> getProperties() {
        return new ConvertingIterable<InMemOntologyProperty, OntologyProperty>(propertiesCache.asMap().values()) {
            @Override
            protected OntologyProperty convert(InMemOntologyProperty ontologyProperty) {
                return ontologyProperty;
            }
        };
    }

    @Override
    public Iterable<Concept> getConcepts() {
        return new ConvertingIterable<InMemConcept, Concept>(conceptsCache.asMap().values()) {
            @Override
            protected Concept convert(InMemConcept concept) {
                return concept;
            }
        };
    }

    @Override
    public String getDisplayNameForLabel(String relationshipIRI) {
        return relationshipsCache.asMap().get(relationshipIRI).getDisplayName();
    }

    @Override
    public OntologyProperty getProperty(String propertyIRI) {
        return propertiesCache.asMap().get(propertyIRI);
    }

    @Override
    public Relationship getRelationshipByIRI(String relationshipIRI) {
        return relationshipsCache.asMap().get(relationshipIRI);
    }

    @Override
    public Iterable<Concept> getConceptsWithProperties() {
        return new ConvertingIterable<InMemConcept, Concept>(conceptsCache.asMap().values()) {
            @Override
            protected Concept convert(InMemConcept concept) {
                return concept;
            }
        };
    }

    @Override
    public Concept getEntityConcept() {
        return conceptsCache.asMap().get(ReadOnlyInMemOntologyRepository.ENTITY_CONCEPT_IRI);
    }

    @Override
    public Concept getParentConcept(Concept concept) {
        ConcurrentMap<String, InMemConcept> conceptConcurrentMap = conceptsCache.asMap();
        for (String key : conceptConcurrentMap.keySet()) {
            if (key.equals(concept.getParentConceptIRI())) {
                return conceptConcurrentMap.get(key);
            }
        }
        return null;
    }

    @Override
    public Concept getConceptByIRI(String conceptIRI) {
        ConcurrentMap<String, InMemConcept> conceptConcurrentMap = conceptsCache.asMap();
        for (String key : conceptConcurrentMap.keySet()) {
            if (key.equals(conceptIRI)) {
                return conceptConcurrentMap.get(key);
            }
        }
        return null;
    }

    @Override
    public List<Concept> getConceptAndChildrenByIRI(String conceptIRI) {
        List<Concept> concepts = new ArrayList<Concept>();
        concepts.add(conceptsCache.asMap().get(conceptIRI));
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        try {
            List<OWLOntology> owlOntologyList = loadOntologyFiles(m, owlConfig, null);
            OWLClass owlClass = m.getOWLDataFactory().getOWLClass(IRI.create(conceptIRI));
            for (OWLClassExpression child : owlClass.getSubClasses(new HashSet<OWLOntology>(owlOntologyList))) {
                InMemConcept inMemConcept = conceptsCache.asMap().get(child.asOWLClass().getIRI().toString());
                concepts.add(inMemConcept);
            }
        } catch (Exception e) {
            throw new LumifyException("could not load ontology files");
        }


        return concepts;
    }

    @Override
    public List<Concept> getAllLeafNodesByConcept(Concept concept) {
        List<Concept> concepts = new ArrayList<Concept>();
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();

        try {
            List<OWLOntology> owlOntologyList = loadOntologyFiles(m, owlConfig, null);
            OWLClass owlClass = m.getOWLDataFactory().getOWLClass(IRI.create(((InMemConcept) concept).getConceptIRI()));
            for (OWLClassExpression child : owlClass.getSubClasses(new HashSet<OWLOntology>(owlOntologyList))) {
                InMemConcept inMemConcept = conceptsCache.asMap().get(child.asOWLClass().getIRI().toString());
                concepts.add(inMemConcept);
            }
        } catch (Exception e) {
            throw new LumifyException("could not load ontology files");
        }

        return concepts;
    }

    @Override
    public Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName) {
        InMemConcept concept = (InMemConcept) getConceptByIRI(conceptIRI);
        if (concept != null) {
            return concept;
        }
        InMemOntologyProperty inMemOntologyProperty = new InMemOntologyProperty();
        inMemOntologyProperty.setDisplayName(displayName);
        Collection<OntologyProperty> ontologyProperties = new ArrayList<OntologyProperty>();
        ontologyProperties.add(inMemOntologyProperty);

        if (parent == null) {
            concept = new InMemConcept(conceptIRI, null, ontologyProperties);
        } else {
            concept = new InMemConcept(conceptIRI, ((InMemConcept) parent).getConceptIRI(), ontologyProperties);
        }
        concept.setTitle(conceptIRI);
        concept.setDisplayName(displayName);
        conceptsCache.put(conceptIRI, concept);

        return concept;
    }

    @Override
    public Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipIRI, String displayName) {
        Relationship relationship = getRelationshipByIRI(relationshipIRI);
        if (relationship != null) {
            return relationship;
        }

        InMemRelationship inMemRelationship = new InMemRelationship(relationshipIRI, displayName, ((InMemConcept) from).getConceptIRI(), ((InMemConcept) to).getConceptIRI());
        relationshipsCache.put(relationshipIRI, inMemRelationship);
        return inMemRelationship;
    }

    @Override
    public void resolvePropertyIds(JSONArray filterJson) throws JSONException {

    }
}
