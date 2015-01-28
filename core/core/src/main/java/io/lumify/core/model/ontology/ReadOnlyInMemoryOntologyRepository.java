package io.lumify.core.model.ontology;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.PropertyType;
import org.apache.commons.io.IOUtils;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.TextIndexHint;
import org.securegraph.inmemory.InMemoryAuthorizations;
import org.securegraph.util.ConvertingIterable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.toList;

public class ReadOnlyInMemoryOntologyRepository extends OntologyRepositoryBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ReadOnlyInMemoryOntologyRepository.class);
    private final Graph graph;
    private final OWLOntologyLoaderConfiguration owlConfig = new OWLOntologyLoaderConfiguration();
    private final Map<String, InMemoryConcept> conceptsCache = new HashMap<>();
    private final Map<String, InMemoryOntologyProperty> propertiesCache = new HashMap<>();
    private final Map<String, InMemoryRelationship> relationshipsCache = new HashMap<>();
    private final List<OwlData> fileCache = new ArrayList<>();

    @Inject
    public ReadOnlyInMemoryOntologyRepository(
            final Graph graph,
            final Configuration configuration
    ) throws Exception {
        super(configuration);
        this.graph = graph;

        clearCache();
        Authorizations authorizations = new InMemoryAuthorizations(VISIBILITY_STRING);
        owlConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
        if (!isOntologyDefined()) {
            LOGGER.info("Base ontology not defined. Creating a new ontology.");
            defineOntology(getConfiguration(), authorizations);
        } else {
            LOGGER.info("Base ontology already defined.");
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
                try (InputStream iconFileIn = new FileInputStream(iconFile)) {
                    concept.setProperty(propertyKey, IOUtils.toByteArray(iconFileIn), authorizations);
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
        List<OWLOntology> loadedOntologies = new ArrayList<>();
        for (OwlData owlData : fileCache) {
            IRI lumifyBaseOntologyIRI = IRI.create(owlData.iri);
            if (excludedIRI != null && excludedIRI.equals(lumifyBaseOntologyIRI)) {
                continue;
            }
            try (InputStream lumifyBaseOntologyIn = new ByteArrayInputStream(owlData.data)) {
                Reader lumifyBaseOntologyReader = new InputStreamReader(lumifyBaseOntologyIn);
                LOGGER.info("Loading existing ontology: %s", owlData.iri);
                OWLOntologyDocumentSource lumifyBaseOntologySource = new ReaderDocumentSource(lumifyBaseOntologyReader, lumifyBaseOntologyIRI);
                OWLOntology o = m.loadOntologyFromOntologyDocument(lumifyBaseOntologySource, config);
                loadedOntologies.add(o);
            }
        }
        return loadedOntologies;
    }

    @Override
    protected OntologyProperty addPropertyTo(
            List<Concept> concepts,
            String propertyIri,
            String displayName,
            PropertyType dataType,
            Map<String, String> possibleValues,
            Collection<TextIndexHint> textIndexHints,
            boolean userVisible,
            boolean searchable,
            String displayType,
            String propertyGroup,
            Double boost,
            String[] intents) {
        checkNotNull(concepts, "concept was null");
        InMemoryOntologyProperty property = getOrCreatePropertyType(propertyIri, dataType, displayName, possibleValues, textIndexHints, userVisible, searchable, displayType, propertyGroup, boost, intents);
        for (Concept concept : concepts) {
            concept.getProperties().add(property);
        }
        checkNotNull(property, "Could not find property: " + propertyIri);
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
            final String propertyIri,
            final PropertyType dataType,
            final String displayName,
            Map<String, String> possibleValues,
            Collection<TextIndexHint> textIndexHints,
            boolean userVisible,
            boolean searchable,
            String displayType,
            String propertyGroup,
            Double boost,
            String[] intents) {
        InMemoryOntologyProperty property = (InMemoryOntologyProperty) getPropertyByIRI(propertyIri);
        if (property == null) {
            definePropertyOnGraph(graph, propertyIri, dataType, textIndexHints, boost);

            property = new InMemoryOntologyProperty();
            property.setDataType(dataType);
            property.setUserVisible(userVisible);
            property.setSearchable(searchable);
            property.setTitle(propertyIri);
            property.setBoost(boost);
            property.setDisplayType(displayType);
            property.setPropertyGroup(propertyGroup);
            for (String intent : intents) {
                property.addIntent(intent);
            }
            if (displayName != null && !displayName.trim().isEmpty()) {
                property.setDisplayName(displayName);
            }
            property.setPossibleValues(possibleValues);
            propertiesCache.put(propertyIri, property);
        }
        return property;
    }

    @Override
    public void clearCache() {
        // do nothing it's all in memory already.
    }

    @Override
    public Iterable<Relationship> getRelationships() {
        return new ConvertingIterable<InMemoryRelationship, Relationship>(relationshipsCache.values()) {
            @Override
            protected Relationship convert(InMemoryRelationship InMemRelationship) {
                return InMemRelationship;
            }
        };
    }

    @Override
    public Iterable<OntologyProperty> getProperties() {
        return new ConvertingIterable<InMemoryOntologyProperty, OntologyProperty>(propertiesCache.values()) {
            @Override
            protected OntologyProperty convert(InMemoryOntologyProperty ontologyProperty) {
                return ontologyProperty;
            }
        };
    }

    @Override
    public String getDisplayNameForLabel(String relationshipIRI) {
        InMemoryRelationship relationship = relationshipsCache.get(relationshipIRI);
        checkNotNull(relationship, "Could not find relationship " + relationshipIRI);
        return relationship.getDisplayName();
    }

    @Override
    public OntologyProperty getPropertyByIRI(String propertyIRI) {
        return propertiesCache.get(propertyIRI);
    }

    @Override
    public Relationship getRelationshipByIRI(String relationshipIRI) {
        return relationshipsCache.get(relationshipIRI);
    }

    @Override
    public boolean hasRelationshipByIRI(String relationshipIRI) {
        return getRelationshipByIRI(relationshipIRI) != null;
    }

    @Override
    public Iterable<Concept> getConceptsWithProperties() {
        return new ConvertingIterable<InMemoryConcept, Concept>(conceptsCache.values()) {
            @Override
            protected Concept convert(InMemoryConcept concept) {
                return concept;
            }
        };
    }

    @Override
    public Concept getEntityConcept() {
        return conceptsCache.get(ReadOnlyInMemoryOntologyRepository.ENTITY_CONCEPT_IRI);
    }

    @Override
    public Concept getParentConcept(Concept concept) {
        for (String key : conceptsCache.keySet()) {
            if (key.equals(concept.getParentConceptIRI())) {
                return conceptsCache.get(key);
            }
        }
        return null;
    }

    @Override
    public List<Concept> getConceptAndChildrenByIRI(String conceptIRI) {
        List<Concept> concepts = new ArrayList<>();
        concepts.add(conceptsCache.get(conceptIRI));
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        try {
            List<OWLOntology> owlOntologyList = loadOntologyFiles(m, owlConfig, null);
            OWLClass owlClass = m.getOWLDataFactory().getOWLClass(IRI.create(conceptIRI));
            for (OWLClassExpression child : owlClass.getSubClasses(new HashSet<>(owlOntologyList))) {
                InMemoryConcept inMemoryConcept = conceptsCache.get(child.asOWLClass().getIRI().toString());
                concepts.add(inMemoryConcept);
            }
        } catch (Exception e) {
            throw new LumifyException("could not load ontology files");
        }


        return concepts;
    }

    @Override
    public List<Concept> getAllLeafNodesByConcept(Concept concept) {
        List<Concept> concepts = Lists.newArrayList(concept);
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();

        try {
            List<OWLOntology> owlOntologyList = loadOntologyFiles(m, owlConfig, null);
            OWLClass owlClass = m.getOWLDataFactory().getOWLClass(IRI.create(((InMemoryConcept) concept).getConceptIRI()));
            for (OWLClassExpression child : owlClass.getSubClasses(new HashSet<>(owlOntologyList))) {
                InMemoryConcept inMemoryConcept = conceptsCache.get(child.asOWLClass().getIRI().toString());
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
    public Relationship getOrCreateRelationshipType(Iterable<Concept> domainConcepts, Iterable<Concept> rangeConcepts, String relationshipIRI, String displayName, String[] intents) {
        Relationship relationship = getRelationshipByIRI(relationshipIRI);
        if (relationship != null) {
            return relationship;
        }

        List<String> domainConceptIris = toList(new ConvertingIterable<Concept, String>(domainConcepts) {
            @Override
            protected String convert(Concept o) {
                return o.getIRI();
            }
        });

        List<String> rangeConceptIris = toList(new ConvertingIterable<Concept, String>(rangeConcepts) {
            @Override
            protected String convert(Concept o) {
                return o.getIRI();
            }
        });

        InMemoryRelationship inMemRelationship = new InMemoryRelationship(relationshipIRI, displayName, domainConceptIris, rangeConceptIris, intents);
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
