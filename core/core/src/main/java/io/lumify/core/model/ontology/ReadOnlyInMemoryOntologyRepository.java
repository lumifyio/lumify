package io.lumify.core.model.ontology;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.exception.LumifyResourceNotFoundException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.TextIndexHint;
import org.securegraph.inmemory.InMemoryAuthorizations;
import org.securegraph.util.ConvertingIterable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class ReadOnlyInMemoryOntologyRepository extends OntologyRepositoryBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ReadOnlyInMemoryOntologyRepository.class);
    private OWLOntologyLoaderConfiguration owlConfig = new OWLOntologyLoaderConfiguration();
    private Cache<String, InMemoryConcept> conceptsCache = CacheBuilder.newBuilder()
            .build();
    private Cache<String, InMemoryOntologyProperty> propertiesCache = CacheBuilder.newBuilder()
            .build();
    private Cache<String, InMemoryRelationship> relationshipsCache = CacheBuilder.newBuilder()
            .build();
    private List<OwlData> fileCache = new ArrayList<OwlData>();

    public void init(Configuration config) throws Exception {
        Map<String, String> ontologies = config.getSubset(Configuration.ONTOLOGY_REPOSITORY_OWL);
        clearCache();
        Authorizations authorizations = new InMemoryAuthorizations(VISIBILITY_STRING);
        owlConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        if (!isOntologyDefined()) {
            LOGGER.info("Base ontology not defined. Creating a new ontology.");
            defineOntology(authorizations);
        } else {
            LOGGER.info("Base ontology already defined.");
        }

        for (int i = 1; i < 1000; i++) {
            String iri = ontologies.get(i + ".iri");
            String dir = ontologies.get(i + ".dir");
            String file = ontologies.get(i + ".file");

            if (iri != null) {
                if (dir != null) {
                    File owlFile = findOwlFile(new File(dir));
                    if (owlFile == null) {
                        throw new LumifyResourceNotFoundException("could not find owl file in directory " + new File(dir).getAbsolutePath());
                    }
                    importFile(owlFile, IRI.create(iri), authorizations);
                } else if (file != null) {
                    writePackage(new File(file), IRI.create(iri), authorizations);
                } else {
                    throw new LumifyResourceNotFoundException("iri without file or dir");
                }
            }
        }
    }

    @Override
    protected Concept importOntologyClass(OWLOntology o, OWLClass ontologyClass, File inDir, Authorizations authorizations) throws IOException {
        InMemoryConcept concept = (InMemoryConcept) super.importOntologyClass(o, ontologyClass, inDir, authorizations);
        conceptsCache.put(concept.getIRI(), concept);
        return concept;
    }

    @Override
    protected void setIconProperty(Concept concept, File inDir, String glyphIconFileName, String propertyKey, Authorizations authorizations) throws IOException {
        if (glyphIconFileName == null) {
            concept.setProperty(propertyKey, null, authorizations);
        } else {
            File iconFile = new File(inDir, glyphIconFileName);
            if (!iconFile.exists()) {
                throw new RuntimeException("Could not find icon file: " + iconFile.toString());
            }
            try {
                InputStream iconFileIn = new FileInputStream(iconFile);
                try {
                    concept.setProperty(propertyKey, IOUtils.toByteArray(iconFileIn), authorizations);
                } finally {
                    iconFileIn.close();
                }
            } catch (IOException ex) {
                throw new LumifyException("Failed to set glyph icon to " + iconFile, ex);
            }
        }
    }

    @Override
    protected void addEntityGlyphIconToEntityConcept(Concept entityConcept, byte[] rawImg) {
        entityConcept.setProperty(LumifyProperties.GLYPH_ICON.getPropertyName(), rawImg, null);
    }

    @Override
    protected void storeOntologyFile(InputStream inputStream, IRI documentIRI) {
        try {
            byte[] inFileData = IOUtils.toByteArray(inputStream);
            fileCache.add(new OwlData(documentIRI.toString(), inFileData));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected List<OWLOntology> loadOntologyFiles(OWLOntologyManager m, OWLOntologyLoaderConfiguration config, IRI excludedIRI) throws Exception {
        List<OWLOntology> loadedOntologies = new ArrayList<OWLOntology>();
        for (OwlData owlData : fileCache) {
            IRI lumifyBaseOntologyIRI = IRI.create(owlData.iri);
            if (excludedIRI != null && excludedIRI.equals(lumifyBaseOntologyIRI)) {
                continue;
            }
            InputStream lumifyBaseOntologyIn = new ByteArrayInputStream(owlData.data);
            try {
                Reader lumifyBaseOntologyReader = new InputStreamReader(lumifyBaseOntologyIn);
                LOGGER.info("Loading existing ontology: %s", owlData.iri);
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
    protected OntologyProperty addPropertyTo(
            Concept concept,
            String propertyIRI,
            String displayName,
            PropertyType dataType,
            JSONObject possibleValues,
            Collection<TextIndexHint> textIndexHints,
            boolean userVisible,
            boolean searchable,
            Boolean displayTime,
            Double boost) {
        checkNotNull(concept, "concept was null");
        InMemoryOntologyProperty property = getOrCreatePropertyType(propertyIRI, dataType, displayName, possibleValues, userVisible, searchable, displayTime, boost);
        concept.getProperties().add(property);
        checkNotNull(property, "Could not find property: " + propertyIRI);
        return property;
    }

    @Override
    protected void getOrCreateInverseOfRelationship(Relationship fromRelationship, Relationship inverseOfRelationship) {
        InMemoryRelationship fromRelationshipMem = (InMemoryRelationship) fromRelationship;
        InMemoryRelationship inverseOfRelationshipMem = (InMemoryRelationship) inverseOfRelationship;

        fromRelationshipMem.addInverseOf(inverseOfRelationshipMem);
        inverseOfRelationshipMem.addInverseOf(fromRelationshipMem);
    }

    private InMemoryOntologyProperty getOrCreatePropertyType(
            final String propertyName,
            final PropertyType dataType,
            final String displayName,
            JSONObject possibleValues,
            boolean userVisible,
            boolean searchable,
            Boolean displayTime,
            Double boost) {
        InMemoryOntologyProperty property = (InMemoryOntologyProperty) getProperty(propertyName);
        if (property == null) {
            property = new InMemoryOntologyProperty();
            property.setDataType(dataType);
            property.setUserVisible(userVisible);
            property.setSearchable(searchable);
            property.setTitle(propertyName);
            property.setDisplayTime(displayTime);
            property.setBoost(boost);
            if (displayName != null && !displayName.trim().isEmpty()) {
                property.setDisplayName(displayName);
            }
            property.setPossibleValues(possibleValues);
            propertiesCache.put(propertyName, property);
        }
        return property;
    }

    @Override
    public void clearCache() {
        LOGGER.info("clearing ReadOnlyInMemoryOntologyRepository cache");
        propertiesCache.invalidateAll();
        relationshipsCache.invalidateAll();
        conceptsCache.invalidateAll();
    }

    @Override
    public Iterable<Relationship> getRelationshipLabels() {
        return new ConvertingIterable<InMemoryRelationship, Relationship>(relationshipsCache.asMap().values()) {
            @Override
            protected Relationship convert(InMemoryRelationship InMemRelationship) {
                return InMemRelationship;
            }
        };
    }

    @Override
    public Iterable<OntologyProperty> getProperties() {
        return new ConvertingIterable<InMemoryOntologyProperty, OntologyProperty>(propertiesCache.asMap().values()) {
            @Override
            protected OntologyProperty convert(InMemoryOntologyProperty ontologyProperty) {
                return ontologyProperty;
            }
        };
    }

    @Override
    public Iterable<Concept> getConcepts() {
        return new ConvertingIterable<InMemoryConcept, Concept>(conceptsCache.asMap().values()) {
            @Override
            protected Concept convert(InMemoryConcept concept) {
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
        return new ConvertingIterable<InMemoryConcept, Concept>(conceptsCache.asMap().values()) {
            @Override
            protected Concept convert(InMemoryConcept concept) {
                return concept;
            }
        };
    }

    @Override
    public Concept getEntityConcept() {
        return conceptsCache.asMap().get(ReadOnlyInMemoryOntologyRepository.ENTITY_CONCEPT_IRI);
    }

    @Override
    public Concept getParentConcept(Concept concept) {
        ConcurrentMap<String, InMemoryConcept> conceptConcurrentMap = conceptsCache.asMap();
        for (String key : conceptConcurrentMap.keySet()) {
            if (key.equals(concept.getParentConceptIRI())) {
                return conceptConcurrentMap.get(key);
            }
        }
        return null;
    }

    @Override
    public Concept getConceptByIRI(String conceptIRI) {
        ConcurrentMap<String, InMemoryConcept> conceptConcurrentMap = conceptsCache.asMap();
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
                InMemoryConcept inMemoryConcept = conceptsCache.asMap().get(child.asOWLClass().getIRI().toString());
                concepts.add(inMemoryConcept);
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
            OWLClass owlClass = m.getOWLDataFactory().getOWLClass(IRI.create(((InMemoryConcept) concept).getConceptIRI()));
            for (OWLClassExpression child : owlClass.getSubClasses(new HashSet<OWLOntology>(owlOntologyList))) {
                InMemoryConcept inMemoryConcept = conceptsCache.asMap().get(child.asOWLClass().getIRI().toString());
                concepts.add(inMemoryConcept);
            }
        } catch (Exception e) {
            throw new LumifyException("could not load ontology files");
        }

        return concepts;
    }

    @Override
    public Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir) {
        InMemoryConcept concept = (InMemoryConcept) getConceptByIRI(conceptIRI);
        if (concept != null) {
            return concept;
        }
        if (parent == null) {
            concept = new InMemoryConcept(conceptIRI, null);
        } else {
            concept = new InMemoryConcept(conceptIRI, ((InMemoryConcept) parent).getConceptIRI());
        }
        concept.setProperty(LumifyProperties.TITLE.getPropertyName(), conceptIRI, null);
        concept.setProperty(LumifyProperties.DISPLAY_NAME.getPropertyName(), displayName, null);
        conceptsCache.put(conceptIRI, concept);

        return concept;
    }

    @Override
    public Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipIRI, String displayName) {
        Relationship relationship = getRelationshipByIRI(relationshipIRI);
        if (relationship != null) {
            return relationship;
        }

        InMemoryRelationship inMemRelationship = new InMemoryRelationship(relationshipIRI, displayName, ((InMemoryConcept) from).getConceptIRI(), ((InMemoryConcept) to).getConceptIRI());
        relationshipsCache.put(relationshipIRI, inMemRelationship);
        return inMemRelationship;
    }

    private static class OwlData {
        public final String iri;
        public final byte[] data;

        public OwlData(String iri, byte[] data) {
            this.iri = iri;
            this.data = data;
        }
    }
}
