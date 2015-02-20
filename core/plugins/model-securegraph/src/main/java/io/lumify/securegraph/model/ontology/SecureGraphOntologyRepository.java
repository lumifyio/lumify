package io.lumify.securegraph.model.ontology;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.ontology.*;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.util.JSONUtil;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.core.util.TimingCallable;
import io.lumify.web.clientapi.model.ClientApiOntology;
import io.lumify.web.clientapi.model.PropertyType;
import org.securegraph.*;
import org.securegraph.property.StreamingPropertyValue;
import org.securegraph.util.ConvertingIterable;
import org.securegraph.util.FilterIterable;
import org.securegraph.util.IterableUtils;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.lumify.core.model.properties.LumifyProperties.*;
import static org.securegraph.util.IterableUtils.singleOrDefault;
import static org.securegraph.util.IterableUtils.toList;

@Singleton
public class SecureGraphOntologyRepository extends OntologyRepositoryBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SecureGraphOntologyRepository.class);
    public static final String ID_PREFIX = "ontology_";
    public static final String ID_PREFIX_PROPERTY = ID_PREFIX + "prop_";
    public static final String ID_PREFIX_RELATIONSHIP = ID_PREFIX + "rel_";
    public static final String ID_PREFIX_CONCEPT = ID_PREFIX + "concept_";
    private static final int QUERY_LIMIT = 10000;
    public static final String ONTOLOGY_FILE_PROPERTY_NAME = "http://lumify.io#ontologyFile";
    private Graph graph;
    private Authorizations authorizations;
    private Cache<String, List<Concept>> allConceptsWithPropertiesCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.HOURS)
            .build();
    private Cache<String, List<OntologyProperty>> allPropertiesCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.HOURS)
            .build();
    private Cache<String, List<Relationship>> relationshipLabelsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.HOURS)
            .build();
    private Cache<String, ClientApiOntology> clientApiCache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.HOURS)
            .build();

    @Inject
    public SecureGraphOntologyRepository(
            final Graph graph,
            final Configuration config,
            final AuthorizationRepository authorizationRepository) throws Exception {
        super(config);
        this.graph = graph;

        authorizationRepository.addAuthorizationToGraph(VISIBILITY_STRING);

        Set<String> authorizationsSet = new HashSet<>();
        authorizationsSet.add(VISIBILITY_STRING);
        this.authorizations = authorizationRepository.createAuthorizations(authorizationsSet);

        loadOntologies(config, authorizations);
    }

    @Override
    protected void importOntologyAnnotationProperty(OWLOntology o, OWLAnnotationProperty annotationProperty, File inDir, Authorizations authorizations) {
        super.importOntologyAnnotationProperty(o, annotationProperty, inDir, authorizations);

        String about = annotationProperty.getIRI().toString();
        LOGGER.debug("disabling index for annotation property: " + about);
        DefinePropertyBuilder definePropertyBuilder = graph.defineProperty(about);
        definePropertyBuilder.dataType(PropertyType.getTypeClass(PropertyType.STRING));
        definePropertyBuilder.textIndexHint(TextIndexHint.NONE);
        definePropertyBuilder.define();
    }

    @Override
    public ClientApiOntology getClientApiObject() {
        ClientApiOntology o = this.clientApiCache.getIfPresent("clientApi");
        if (o != null) {
            return o;
        }
        o = super.getClientApiObject();
        this.clientApiCache.put("clientApi", o);
        return o;
    }

    @Override
    public void clearCache() {
        LOGGER.info("clearing ontology cache");
        graph.flush();
        this.clientApiCache.invalidateAll();
        this.allConceptsWithPropertiesCache.invalidateAll();
        this.allPropertiesCache.invalidateAll();
        this.relationshipLabelsCache.invalidateAll();
    }

    @Override
    protected void addEntityGlyphIconToEntityConcept(Concept entityConcept, byte[] rawImg) {
        StreamingPropertyValue raw = new StreamingPropertyValue(new ByteArrayInputStream(rawImg), byte[].class);
        raw.searchIndex(false);
        entityConcept.setProperty(LumifyProperties.GLYPH_ICON.getPropertyName(), raw, authorizations);
        graph.flush();
    }

    @Override
    public void storeOntologyFile(InputStream in, IRI documentIRI) {
        StreamingPropertyValue value = new StreamingPropertyValue(in, byte[].class);
        value.searchIndex(false);
        Metadata metadata = new Metadata();
        Vertex rootConceptVertex = ((SecureGraphConcept) getRootConcept()).getVertex();
        metadata.add("index", toList(rootConceptVertex.getProperties(ONTOLOGY_FILE_PROPERTY_NAME)).size(), VISIBILITY.getVisibility());
        rootConceptVertex.addPropertyValue(documentIRI.toString(), ONTOLOGY_FILE_PROPERTY_NAME, value, metadata, VISIBILITY.getVisibility(), authorizations);
        graph.flush();
    }

    @Override
    protected boolean isOntologyDefined(String iri) {
        Vertex rootConceptVertex = ((SecureGraphConcept) getRootConcept()).getVertex();
        Property prop = rootConceptVertex.getProperty(iri, ONTOLOGY_FILE_PROPERTY_NAME);
        if (prop == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<OWLOntology> loadOntologyFiles(OWLOntologyManager m, OWLOntologyLoaderConfiguration config, IRI excludedIRI) throws OWLOntologyCreationException, IOException {
        List<OWLOntology> loadedOntologies = new ArrayList<>();
        Iterable<Property> ontologyFiles = getOntologyFiles();
        for (Property ontologyFile : ontologyFiles) {
            IRI ontologyFileIRI = IRI.create(ontologyFile.getKey());
            if (excludedIRI != null && excludedIRI.equals(ontologyFileIRI)) {
                continue;
            }
            try (InputStream lumifyBaseOntologyIn = ((StreamingPropertyValue) ontologyFile.getValue()).getInputStream()) {
                Reader lumifyBaseOntologyReader = new InputStreamReader(lumifyBaseOntologyIn);
                LOGGER.info("Loading existing ontology: %s", ontologyFile.getKey());
                OWLOntologyDocumentSource lumifyBaseOntologySource = new ReaderDocumentSource(lumifyBaseOntologyReader, ontologyFileIRI);
                try {
                    OWLOntology o = m.loadOntologyFromOntologyDocument(lumifyBaseOntologySource, config);
                    loadedOntologies.add(o);
                } catch (UnloadableImportException ex) {
                    LOGGER.error("Could not load %s", ontologyFileIRI, ex);
                }
            }
        }
        return loadedOntologies;
    }

    private Iterable<Property> getOntologyFiles() {
        SecureGraphConcept rootConcept = (SecureGraphConcept) getRootConcept();
        checkNotNull(rootConcept, "Could not get root concept");
        Vertex rootConceptVertex = rootConcept.getVertex();
        checkNotNull(rootConceptVertex, "Could not get root concept vertex");

        List<Property> ontologyFiles = toList(rootConceptVertex.getProperties(ONTOLOGY_FILE_PROPERTY_NAME));
        Collections.sort(ontologyFiles, new Comparator<Property>() {
            @Override
            public int compare(Property ontologyFile1, Property ontologyFile2) {
                Integer index1 = (Integer) ontologyFile1.getMetadata().getValue("index");
                checkNotNull(index1, "Could not find metadata (1) 'index' on " + ontologyFile1);
                Integer index2 = (Integer) ontologyFile2.getMetadata().getValue("index");
                checkNotNull(index2, "Could not find metadata (2) 'index' on " + ontologyFile2);
                return index1.compareTo(index2);
            }
        });
        return ontologyFiles;
    }

    @Override
    public Iterable<Relationship> getRelationships() {
        try {
            return relationshipLabelsCache.get("", new TimingCallable<List<Relationship>>("getRelationships") {
                @Override
                public List<Relationship> callWithTime() throws Exception {
                    Iterable<Vertex> vertices = graph.query(getAuthorizations())
                            .has(CONCEPT_TYPE.getPropertyName(), TYPE_RELATIONSHIP)
                            .limit(QUERY_LIMIT)
                            .vertices();

                    return toList(new ConvertingIterable<Vertex, Relationship>(vertices) {
                        @Override
                        protected Relationship convert(Vertex vertex) {
                            return toSecureGraphRelationship(vertex);
                        }
                    });
                }
            });
        } catch (ExecutionException e) {
            throw new LumifyException("Could not get relationship labels");
        }
    }

    private Relationship toSecureGraphRelationship(Vertex relationshipVertex) {
        Iterable<Vertex> domainVertices = relationshipVertex.getVertices(Direction.IN, LabelName.HAS_EDGE.toString(), getAuthorizations());
        List<String> domainConceptIris = toList(new ConvertingIterable<Vertex, String>(domainVertices) {
            @Override
            protected String convert(Vertex domainVertex) {
                return ONTOLOGY_TITLE.getPropertyValue(domainVertex);
            }
        });

        Iterable<Vertex> rangeVertices = relationshipVertex.getVertices(Direction.OUT, LabelName.HAS_EDGE.toString(), getAuthorizations());
        List<String> rangeConceptIris = toList(new ConvertingIterable<Vertex, String>(rangeVertices) {
            @Override
            protected String convert(Vertex rangeVertex) {
                return ONTOLOGY_TITLE.getPropertyValue(rangeVertex);
            }
        });

        final List<String> inverseOfIRIs = getRelationshipInverseOfIRIs(relationshipVertex);
        return new SecureGraphRelationship(relationshipVertex, domainConceptIris, rangeConceptIris, inverseOfIRIs);
    }

    private List<String> getRelationshipInverseOfIRIs(final Vertex vertex) {
        return IterableUtils.toList(new ConvertingIterable<Vertex, String>(vertex.getVertices(Direction.OUT, LabelName.INVERSE_OF.toString(), getAuthorizations())) {
            @Override
            protected String convert(Vertex inverseOfVertex) {
                return LumifyProperties.ONTOLOGY_TITLE.getPropertyValue(inverseOfVertex);
            }
        });
    }

    @Override
    public String getDisplayNameForLabel(String relationshipIRI) {
        String displayName = null;
        if (relationshipIRI != null && !relationshipIRI.trim().isEmpty()) {
            try {
                Relationship relationship = getRelationshipByIRI(relationshipIRI);
                if (relationship != null) {
                    displayName = relationship.getDisplayName();
                }
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException(String.format("Found multiple vertices for relationship label \"%s\"", relationshipIRI),
                        iae);
            }
        }
        return displayName;
    }

    @Override
    public Iterable<OntologyProperty> getProperties() {
        try {
            return allPropertiesCache.get("", new TimingCallable<List<OntologyProperty>>("getProperties") {
                @Override
                public List<OntologyProperty> callWithTime() throws Exception {
                    return toList(new ConvertingIterable<Vertex, OntologyProperty>(graph.query(getAuthorizations())
                            .has(CONCEPT_TYPE.getPropertyName(), TYPE_PROPERTY)
                            .limit(QUERY_LIMIT)
                            .vertices()) {
                        @Override
                        protected OntologyProperty convert(Vertex vertex) {
                            return new SecureGraphOntologyProperty(vertex);
                        }
                    });
                }
            });
        } catch (ExecutionException e) {
            throw new LumifyException("Could not get properties", e);
        }
    }

    @Override
    public boolean hasRelationshipByIRI(String relationshipIRI) {
        return getRelationshipByIRI(relationshipIRI) != null;
    }

    @Override
    public Iterable<Concept> getConceptsWithProperties() {
        try {
            return allConceptsWithPropertiesCache.get("", new TimingCallable<List<Concept>>("getConceptsWithProperties") {
                @Override
                public List<Concept> callWithTime() throws Exception {
                    return toList(new ConvertingIterable<Vertex, Concept>(graph.query(getAuthorizations())
                            .has(CONCEPT_TYPE.getPropertyName(), TYPE_CONCEPT)
                            .limit(QUERY_LIMIT)
                            .vertices()) {
                        @Override
                        protected Concept convert(Vertex vertex) {
                            List<OntologyProperty> conceptProperties = getPropertiesByVertexNoRecursion(vertex);
                            Vertex parentConceptVertex = getParentConceptVertex(vertex);
                            String parentConceptIRI = ONTOLOGY_TITLE.getPropertyValue(parentConceptVertex);
                            return new SecureGraphConcept(vertex, parentConceptIRI, conceptProperties);
                        }
                    });
                }
            });
        } catch (ExecutionException e) {
            throw new LumifyException("could not get concepts with properties", e);
        }
    }

    private Concept getRootConcept() {
        return getConceptByIRI(SecureGraphOntologyRepository.ROOT_CONCEPT_IRI);
    }

    @Override
    public Concept getEntityConcept() {
        return getConceptByIRI(SecureGraphOntologyRepository.ENTITY_CONCEPT_IRI);
    }

    private List<Concept> getChildConcepts(Concept concept) {
        Vertex conceptVertex = ((SecureGraphConcept) concept).getVertex();
        return toConcepts(conceptVertex.getVertices(Direction.IN, LabelName.IS_A.toString(), getAuthorizations()));
    }

    @Override
    public Concept getParentConcept(final Concept concept) {
        Vertex parentConceptVertex = getParentConceptVertex(((SecureGraphConcept) concept).getVertex());
        if (parentConceptVertex == null) {
            return null;
        }
        return new SecureGraphConcept(parentConceptVertex);
    }

    private List<Concept> toConcepts(Iterable<Vertex> vertices) {
        ArrayList<Concept> concepts = new ArrayList<>();
        for (Vertex vertex : vertices) {
            concepts.add(new SecureGraphConcept(vertex));
        }
        return concepts;
    }

    private List<OntologyProperty> getPropertiesByVertexNoRecursion(Vertex vertex) {
        return toList(new ConvertingIterable<Vertex, OntologyProperty>(vertex.getVertices(Direction.OUT, LabelName.HAS_PROPERTY.toString(), getAuthorizations())) {
            @Override
            protected OntologyProperty convert(Vertex o) {
                return new SecureGraphOntologyProperty(o);
            }
        });
    }

    @Override
    public List<Concept> getConceptAndChildrenByIRI(String conceptIRI) {
        ArrayList<Concept> concepts = new ArrayList<>();
        Concept concept = getConceptByIRI(conceptIRI);
        if (concept == null) {
            return null;
        }
        concepts.add(concept);
        List<Concept> children = getChildConcepts(concept);
        concepts.addAll(children);
        return concepts;
    }

    @Override
    public List<Concept> getAllLeafNodesByConcept(Concept concept) {
        List<Concept> childConcepts = getChildConcepts(concept);
        List<Concept> parent = Lists.newArrayList(concept);
        if (childConcepts.size() > 0) {
            List<Concept> childrenList = new ArrayList<>();
            for (Concept childConcept : childConcepts) {
                List<Concept> child = getAllLeafNodesByConcept(childConcept);
                childrenList.addAll(child);
            }
            parent.addAll(childrenList);
        }
        return parent;
    }

    @Override
    public Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName, File inDir) {
        Concept concept = getConceptByIRI(conceptIRI);
        if (concept != null) {
            return concept;
        }

        VertexBuilder builder = graph.prepareVertex(ID_PREFIX_CONCEPT + conceptIRI, VISIBILITY.getVisibility());
        CONCEPT_TYPE.setProperty(builder, TYPE_CONCEPT, VISIBILITY.getVisibility());
        ONTOLOGY_TITLE.setProperty(builder, conceptIRI, VISIBILITY.getVisibility());
        DISPLAY_NAME.setProperty(builder, displayName, VISIBILITY.getVisibility());
        if (conceptIRI.equals(OntologyRepository.ENTITY_CONCEPT_IRI)) {
            LumifyProperties.TITLE_FORMULA.setProperty(builder, "prop('http://lumify.io#title') || ''", VISIBILITY.getVisibility());
            LumifyProperties.SUBTITLE_FORMULA.setProperty(builder, "prop('http://lumify.io#source') || ''", VISIBILITY.getVisibility());
            LumifyProperties.TIME_FORMULA.setProperty(builder, "prop('http://lumify.io#publishedDate') || ''", VISIBILITY.getVisibility());
        }
        Vertex vertex = builder.save(getAuthorizations());

        concept = new SecureGraphConcept(vertex);
        if (parent != null) {
            findOrAddEdge(((SecureGraphConcept) concept).getVertex(), ((SecureGraphConcept) parent).getVertex(), LabelName.IS_A.toString());
        }

        graph.flush();
        return concept;
    }

    protected void findOrAddEdge(Vertex fromVertex, final Vertex toVertex, String edgeLabel) {
        List<Vertex> matchingEdges = toList(new FilterIterable<Vertex>(fromVertex.getVertices(Direction.OUT, edgeLabel, getAuthorizations())) {
            @Override
            protected boolean isIncluded(Vertex vertex) {
                return vertex.getId().equals(toVertex.getId());
            }
        });
        if (matchingEdges.size() > 0) {
            return;
        }
        String edgeId = fromVertex.getId() + "-" + toVertex.getId();
        fromVertex.getGraph().addEdge(edgeId, fromVertex, toVertex, edgeLabel, VISIBILITY.getVisibility(), getAuthorizations());
    }

    @Override
    public OntologyProperty addPropertyTo(
            List<Concept> concepts,
            String propertyIri,
            String displayName,
            PropertyType dataType,
            Map<String, String> possibleValues,
            Collection<TextIndexHint> textIndexHints,
            boolean userVisible,
            boolean searchable,
            boolean addable,
            String displayType,
            String propertyGroup,
            Double boost,
            String validationFormula,
            String displayFormula,
            ImmutableList<String> dependentPropertyIris,
            String[] intents
    ) {
        checkNotNull(concepts, "vertex was null");
        OntologyProperty property = getOrCreatePropertyType(
                concepts,
                propertyIri,
                dataType,
                displayName,
                possibleValues,
                textIndexHints,
                userVisible,
                searchable,
                addable,
                displayType,
                propertyGroup,
                boost,
                validationFormula,
                displayFormula,
                dependentPropertyIris, intents
        );
        checkNotNull(property, "Could not find property: " + propertyIri);

        for (Concept concept : concepts) {
            findOrAddEdge(((SecureGraphConcept) concept).getVertex(), ((SecureGraphOntologyProperty) property).getVertex(), LabelName.HAS_PROPERTY.toString());
        }

        graph.flush();
        return property;
    }

    @Override
    protected void getOrCreateInverseOfRelationship(Relationship fromRelationship, Relationship inverseOfRelationship) {
        checkNotNull(fromRelationship, "fromRelationship is required");
        checkNotNull(fromRelationship, "inverseOfRelationship is required");

        SecureGraphRelationship fromRelationshipSg = (SecureGraphRelationship) fromRelationship;
        SecureGraphRelationship inverseOfRelationshipSg = (SecureGraphRelationship) inverseOfRelationship;

        Vertex fromVertex = fromRelationshipSg.getVertex();
        checkNotNull(fromVertex, "fromVertex is required");

        Vertex inverseVertex = inverseOfRelationshipSg.getVertex();
        checkNotNull(inverseVertex, "inverseVertex is required");

        findOrAddEdge(fromVertex, inverseVertex, LabelName.INVERSE_OF.toString());
        findOrAddEdge(inverseVertex, fromVertex, LabelName.INVERSE_OF.toString());
    }

    @Override
    public Relationship getOrCreateRelationshipType(
            Iterable<Concept> domainConcepts,
            Iterable<Concept> rangeConcepts,
            String relationshipIRI,
            String displayName,
            String[] intents,
            boolean userVisible
    ) {
        Relationship relationship = getRelationshipByIRI(relationshipIRI);
        if (relationship != null) {
            return relationship;
        }

        VertexBuilder builder = graph.prepareVertex(ID_PREFIX_RELATIONSHIP + relationshipIRI, VISIBILITY.getVisibility());
        CONCEPT_TYPE.setProperty(builder, TYPE_RELATIONSHIP, VISIBILITY.getVisibility());
        ONTOLOGY_TITLE.setProperty(builder, relationshipIRI, VISIBILITY.getVisibility());
        DISPLAY_NAME.setProperty(builder, displayName, VISIBILITY.getVisibility());
        USER_VISIBLE.setProperty(builder, userVisible, VISIBILITY.getVisibility());
        for (String intent : intents) {
            INTENT.addPropertyValue(builder, intent, intent, VISIBILITY.getVisibility());
        }
        Vertex relationshipVertex = builder.save(getAuthorizations());

        for (Concept domainConcept : domainConcepts) {
            findOrAddEdge(((SecureGraphConcept) domainConcept).getVertex(), relationshipVertex, LabelName.HAS_EDGE.toString());
        }

        for (Concept rangeConcept : rangeConcepts) {
            findOrAddEdge(relationshipVertex, ((SecureGraphConcept) rangeConcept).getVertex(), LabelName.HAS_EDGE.toString());
        }

        List<String> inverseOfIRIs = new ArrayList<>(); // no inverse of because this relationship is new

        graph.flush();

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

        return new SecureGraphRelationship(relationshipVertex, domainConceptIris, rangeConceptIris, inverseOfIRIs);
    }

    private OntologyProperty getOrCreatePropertyType(
            final List<Concept> concepts,
            final String propertyIri,
            final PropertyType dataType,
            final String displayName,
            Map<String, String> possibleValues,
            Collection<TextIndexHint> textIndexHints,
            boolean userVisible,
            boolean searchable,
            boolean addable,
            String displayType,
            String propertyGroup,
            Double boost,
            String validationFormula,
            String displayFormula,
            ImmutableList<String> dependentPropertyIris,
            String[] intents
    ) {
        OntologyProperty typeProperty = getPropertyByIRI(propertyIri);
        if (typeProperty == null) {
            definePropertyOnGraph(graph, propertyIri, dataType, textIndexHints, boost);

            VertexBuilder builder = graph.prepareVertex(ID_PREFIX_PROPERTY + propertyIri, VISIBILITY.getVisibility());
            CONCEPT_TYPE.setProperty(builder, TYPE_PROPERTY, VISIBILITY.getVisibility());
            ONTOLOGY_TITLE.setProperty(builder, propertyIri, VISIBILITY.getVisibility());
            DATA_TYPE.setProperty(builder, dataType.toString(), VISIBILITY.getVisibility());
            USER_VISIBLE.setProperty(builder, userVisible, VISIBILITY.getVisibility());
            SEARCHABLE.setProperty(builder, searchable, VISIBILITY.getVisibility());
            ADDABLE.setProperty(builder, addable, VISIBILITY.getVisibility());
            if (boost != null) {
                BOOST.setProperty(builder, boost, VISIBILITY.getVisibility());
            }
            if (displayName != null && !displayName.trim().isEmpty()) {
                DISPLAY_NAME.setProperty(builder, displayName.trim(), VISIBILITY.getVisibility());
            }
            if (possibleValues != null) {
                POSSIBLE_VALUES.setProperty(builder, JSONUtil.toJson(possibleValues), VISIBILITY.getVisibility());
            }
            if (displayType != null && !displayType.trim().isEmpty()) {
                DISPLAY_TYPE.setProperty(builder, displayType, VISIBILITY.getVisibility());
            }
            if (propertyGroup != null && !propertyGroup.trim().isEmpty()) {
                PROPERTY_GROUP.setProperty(builder, propertyGroup, VISIBILITY.getVisibility());
            }
            if (validationFormula != null && !validationFormula.trim().isEmpty()) {
                VALIDATION_FORMULA.setProperty(builder, validationFormula, VISIBILITY.getVisibility());
            }
            if (displayFormula != null && !displayFormula.trim().isEmpty()) {
                DISPLAY_FORMULA.setProperty(builder, displayFormula, VISIBILITY.getVisibility());
            }
            if (dependentPropertyIris != null) {
                int i = 0;
                for (String dependentPropertyIri : dependentPropertyIris) {
                    DEPENDENT_PROPERTY_IRI.addPropertyValue(builder, Integer.toString(i), dependentPropertyIri, VISIBILITY.getVisibility());
                    i++;
                }
                // TODO: if the list of dependent property iris gets smaller they will not get cleaned up.
            }
            for (String intent : intents) {
                INTENT.addPropertyValue(builder, intent, intent, VISIBILITY.getVisibility());
            }
            typeProperty = new SecureGraphOntologyProperty(builder.save(getAuthorizations()));
            graph.flush();
        }
        return typeProperty;
    }

    private Vertex getParentConceptVertex(Vertex conceptVertex) {
        try {
            return singleOrDefault(conceptVertex.getVertices(Direction.OUT, LabelName.IS_A.toString(), getAuthorizations()), null);
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException(String.format("Unexpected number of parents for concept %s",
                    TITLE.getPropertyValue(conceptVertex)), iae);
        }
    }

    private Authorizations getAuthorizations() {
        return authorizations;
    }
}
