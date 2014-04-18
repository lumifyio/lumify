package com.altamiracorp.lumify.securegraph.model.ontology;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.ontology.*;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.user.AuthorizationRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.core.util.TimingCallable;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import com.altamiracorp.securegraph.util.FilterIterable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.*;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;
import static com.altamiracorp.lumify.core.util.CollectionUtil.single;
import static com.altamiracorp.securegraph.util.IterableUtils.toList;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class SecureGraphOntologyRepository extends OntologyRepositoryBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SecureGraphOntologyRepository.class);
    private Graph graph;
    private Authorizations authorizations;
    private Cache<String, Concept> conceptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    private Cache<String, List<Concept>> allConceptsWithPropertiesCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    private Cache<String, List<OntologyProperty>> allPropertiesCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    private Cache<String, List<Relationship>> relationshipLabelsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @Inject
    public SecureGraphOntologyRepository(final Graph graph,
                                         final AuthorizationRepository authorizationRepository) {
        this.graph = graph;

        authorizationRepository.addAuthorizationToGraph(SecureGraphOntologyRepository.VISIBILITY_STRING);

        Set<String> authorizationsSet = new HashSet<String>();
        authorizationsSet.add(VISIBILITY_STRING);
        this.authorizations = authorizationRepository.createAuthorizations(authorizationsSet);

        if (!isOntologyDefined()) {
            LOGGER.info("Base ontology not defined. Creating a new ontology.");
            defineOntology();
        } else {
            LOGGER.info("Base ontology already defined.");
        }
    }

    @Override
    public void clearCache() {
        LOGGER.info("clearing ontology cache");
        this.allConceptsWithPropertiesCache.invalidateAll();
        this.allPropertiesCache.invalidateAll();
        this.conceptsCache.invalidateAll();
        this.relationshipLabelsCache.invalidateAll();
    }

    @Override
    protected void addEntityGlyphIconToEntityConcept(Concept entityConcept, byte[] rawImg) {
        StreamingPropertyValue raw = new StreamingPropertyValue(new ByteArrayInputStream(rawImg), byte[].class);
        raw.searchIndex(false);
        entityConcept.setProperty(LumifyProperties.GLYPH_ICON.getKey(), raw, OntologyRepository.VISIBILITY.getVisibility());
        graph.flush();
    }

    @Override
    public void storeOntologyFile(InputStream in, IRI documentIRI) {
        StreamingPropertyValue value = new StreamingPropertyValue(in, byte[].class);
        value.searchIndex(false);
        Map<String, Object> metadata = new HashMap<String, Object>();
        Vertex rootConceptVertex = ((SecureGraphConcept) getRootConcept()).getVertex();
        metadata.put("index", toList(rootConceptVertex.getProperties("ontologyFile")).size());
        rootConceptVertex.addPropertyValue(documentIRI.toString(), "ontologyFile", value, metadata, VISIBILITY.getVisibility());
        graph.flush();
    }

    @Override
    public List<OWLOntology> loadOntologyFiles(OWLOntologyManager m, OWLOntologyLoaderConfiguration config, IRI excludedIRI) throws OWLOntologyCreationException, IOException {
        List<OWLOntology> loadedOntologies = new ArrayList<OWLOntology>();
        Iterable<Property> ontologyFiles = getOntologyFiles();
        for (Property ontologyFile : ontologyFiles) {
            IRI lumifyBaseOntologyIRI = IRI.create(ontologyFile.getKey());
            if (excludedIRI != null && excludedIRI.equals(lumifyBaseOntologyIRI)) {
                continue;
            }
            InputStream lumifyBaseOntologyIn = ((StreamingPropertyValue) ontologyFile.getValue()).getInputStream();
            try {
                Reader lumifyBaseOntologyReader = new InputStreamReader(lumifyBaseOntologyIn);
                LOGGER.info("Loading existing ontology: %s", ontologyFile.getKey());
                OWLOntologyDocumentSource lumifyBaseOntologySource = new ReaderDocumentSource(lumifyBaseOntologyReader, lumifyBaseOntologyIRI);
                OWLOntology o = m.loadOntologyFromOntologyDocument(lumifyBaseOntologySource, config);
                loadedOntologies.add(o);
            } finally {
                lumifyBaseOntologyIn.close();
            }
        }
        return loadedOntologies;
    }

    private Iterable<Property> getOntologyFiles() {
        List<Property> ontologyFiles = toList(((SecureGraphConcept) getRootConcept()).getVertex().getProperties("ontologyFile"));
        Collections.sort(ontologyFiles, new Comparator<Property>() {
            @Override
            public int compare(Property ontologyFile1, Property ontologyFile2) {
                Integer index1 = (Integer) ontologyFile1.getMetadata().get("index");
                Integer index2 = (Integer) ontologyFile2.getMetadata().get("index");
                return index1.compareTo(index2);
            }
        });
        return ontologyFiles;
    }

    @Override
    public Iterable<Relationship> getRelationshipLabels() {
        try {
            return relationshipLabelsCache.get("", new TimingCallable<List<Relationship>>("getRelationshipLabels") {
                @Override
                public List<Relationship> callWithTime() throws Exception {
                    Iterable<Vertex> vertices = graph.query(getAuthorizations())
                            .has(CONCEPT_TYPE.getKey(), TYPE_RELATIONSHIP)
                            .limit(10000)
                            .vertices();

                    return toList(new ConvertingIterable<Vertex, Relationship>(vertices) {
                        @Override
                        protected Relationship convert(Vertex vertex) {
                            String sourceVertexId = single(vertex.getVertexIds(Direction.IN, getAuthorizations())).toString();
                            String destVertexId = single(vertex.getVertexIds(Direction.OUT, getAuthorizations())).toString();
                            return new SecureGraphRelationship(vertex, sourceVertexId, destVertexId);
                        }
                    });
                }
            });
        } catch (ExecutionException e) {
            throw new LumifyException("Could not get relationship labels");
        }
    }

    @Override
    public String getDisplayNameForLabel(String relationshipIRI) {
        String displayName = null;
        if (relationshipIRI != null && !relationshipIRI.trim().isEmpty()) {
            try {
                Vertex relVertex = Iterables.getOnlyElement(graph.query(getAuthorizations())
                        .has(CONCEPT_TYPE.getKey(), TYPE_RELATIONSHIP)
                        .has(ONTOLOGY_TITLE.getKey(), relationshipIRI)
                        .vertices(), null);
                displayName = relVertex != null ? DISPLAY_NAME.getPropertyValue(relVertex) : null;
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
                            .has(CONCEPT_TYPE.getKey(), TYPE_PROPERTY)
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
    public OntologyProperty getProperty(String propertyIRI) {
        try {
            Vertex propVertex = Iterables.getOnlyElement(graph.query(getAuthorizations())
                    .has(CONCEPT_TYPE.getKey(), TYPE_PROPERTY)
                    .has(ONTOLOGY_TITLE.getKey(), propertyIRI)
                    .vertices(), null);
            return propVertex != null ? new SecureGraphOntologyProperty(propVertex) : null;
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException(String.format("Too many \"%s\" properties", propertyIRI), iae);
        }
    }

    @Override
    public Relationship getRelationshipByIRI(String relationshipIRI) {
        Vertex relationshipVertex = graph.getVertex(relationshipIRI, getAuthorizations());
        if (relationshipVertex == null) {
            return null;
        }
        String from = single(relationshipVertex.getVertexIds(Direction.IN, getAuthorizations())).toString();
        String to = single(relationshipVertex.getVertexIds(Direction.OUT, getAuthorizations())).toString();
        return new SecureGraphRelationship(relationshipVertex, from, to);
    }

    @Override
    public Iterable<Concept> getConcepts() {
        return getConcepts(false);
    }

    @Override
    public Iterable<Concept> getConceptsWithProperties() {
        try {
            return allConceptsWithPropertiesCache.get("", new TimingCallable<List<Concept>>("getConceptsWithProperties") {
                @Override
                public List<Concept> callWithTime() throws Exception {
                    return toList(getConcepts(true));
                }
            });
        } catch (ExecutionException e) {
            throw new LumifyException("could not get concepts with properties", e);
        }
    }

    private Iterable<Concept> getConcepts(final boolean withProperties) {
        return new ConvertingIterable<Vertex, Concept>(graph.query(getAuthorizations())
                .has(CONCEPT_TYPE.getKey(), TYPE_CONCEPT)
                .vertices()) {
            @Override
            protected Concept convert(Vertex vertex) {
                if (withProperties) {
                    List<OntologyProperty> conceptProperties = getPropertiesByVertexNoRecursion(vertex);
                    Vertex parentConceptVertex = getParentConceptVertex(vertex);
                    String parentConceptIRI = ONTOLOGY_TITLE.getPropertyValue(parentConceptVertex);
                    return new SecureGraphConcept(vertex, parentConceptIRI, conceptProperties);
                } else {
                    return new SecureGraphConcept(vertex);
                }
            }
        };
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
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        for (Vertex vertex : vertices) {
            concepts.add(new SecureGraphConcept(vertex));
        }
        return concepts;
    }

    @Override
    public Concept getConceptByIRI(String conceptIRI) {
        Vertex conceptVertex = graph.getVertex(conceptIRI, getAuthorizations());
        return conceptVertex != null ? new SecureGraphConcept(conceptVertex) : null;
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
        ArrayList<Concept> concepts = new ArrayList<Concept>();
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
            List<Concept> childrenList = new ArrayList<Concept>();
            for (Concept childConcept : childConcepts) {
                List<Concept> child = getAllLeafNodesByConcept(childConcept);
                childrenList.addAll(child);
            }
            parent.addAll(childrenList);
        }
        return parent;
    }

    @Override
    public Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName) {
        Concept concept = getConceptByIRI(conceptIRI);
        if (concept != null) {
            return concept;
        }

        VertexBuilder builder = graph.prepareVertex(conceptIRI, VISIBILITY.getVisibility(), getAuthorizations());
        CONCEPT_TYPE.setProperty(builder, TYPE_CONCEPT, VISIBILITY.getVisibility());
        ONTOLOGY_TITLE.setProperty(builder, conceptIRI, VISIBILITY.getVisibility());
        DISPLAY_NAME.setProperty(builder, displayName, VISIBILITY.getVisibility());
        Vertex vertex = builder.save();

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
        fromVertex.getGraph().addEdge(fromVertex, toVertex, edgeLabel, VISIBILITY.getVisibility(), getAuthorizations());
    }

    @Override
    public OntologyProperty addPropertyTo(Concept concept, String propertyIRI, String displayName, PropertyType dataType, boolean userVisible) {
        checkNotNull(concept, "vertex was null");
        OntologyProperty property = getOrCreatePropertyType(propertyIRI, dataType, displayName, userVisible);
        checkNotNull(property, "Could not find property: " + propertyIRI);

        findOrAddEdge(((SecureGraphConcept) concept).getVertex(), ((SecureGraphOntologyProperty) property).getVertex(), LabelName.HAS_PROPERTY.toString());

        graph.flush();
        return property;
    }

    @Override
    public Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipIRI, String displayName) {
        Relationship relationship = getRelationshipByIRI(relationshipIRI);
        if (relationship != null) {
            return relationship;
        }

        VertexBuilder builder = graph.prepareVertex(relationshipIRI, VISIBILITY.getVisibility(), getAuthorizations());
        CONCEPT_TYPE.setProperty(builder, TYPE_RELATIONSHIP, VISIBILITY.getVisibility());
        ONTOLOGY_TITLE.setProperty(builder, relationshipIRI, VISIBILITY.getVisibility());
        DISPLAY_NAME.setProperty(builder, displayName, VISIBILITY.getVisibility());
        Vertex relationshipVertex = builder.save();

        findOrAddEdge(((SecureGraphConcept) from).getVertex(), relationshipVertex, LabelName.HAS_EDGE.toString());
        findOrAddEdge(relationshipVertex, ((SecureGraphConcept) to).getVertex(), LabelName.HAS_EDGE.toString());

        graph.flush();
        return new SecureGraphRelationship(relationshipVertex, from.getTitle(), to.getTitle());
    }

    @Override
    public void resolvePropertyIds(JSONArray filterJson) throws JSONException {
        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject filter = filterJson.getJSONObject(i);
            if (filter.has("propertyId") && !filter.has("propertyName")) {
                String propertyVertexId = filter.getString("propertyId");
                OntologyProperty property = getProperty(propertyVertexId);
                if (property == null) {
                    throw new RuntimeException("Could not find property with id: " + propertyVertexId);
                }
                filter.put("propertyName", property.getTitle());
                filter.put("propertyDataType", property.getDataType());
            }
        }
    }

    private OntologyProperty getOrCreatePropertyType(final String propertyName, final PropertyType dataType, final String displayName, boolean userVisible) {
        OntologyProperty typeProperty = getProperty(propertyName);
        if (typeProperty == null) {
            VertexBuilder builder = graph.prepareVertex(VISIBILITY.getVisibility(), getAuthorizations());
            CONCEPT_TYPE.setProperty(builder, TYPE_PROPERTY, VISIBILITY.getVisibility());
            ONTOLOGY_TITLE.setProperty(builder, propertyName, VISIBILITY.getVisibility());
            DATA_TYPE.setProperty(builder, dataType.toString(), VISIBILITY.getVisibility());
            USER_VISIBLE.setProperty(builder, userVisible, VISIBILITY.getVisibility());
            if (displayName != null && !displayName.trim().isEmpty()) {
                DISPLAY_NAME.setProperty(builder, displayName.trim(), VISIBILITY.getVisibility());
            }
            typeProperty = new SecureGraphOntologyProperty(builder.save());
            graph.flush();
        }
        return typeProperty;
    }

    private Vertex getParentConceptVertex(Vertex conceptVertex) {
        try {
            return Iterables.getOnlyElement(conceptVertex.getVertices(Direction.OUT, LabelName.IS_A.toString(), getAuthorizations()), null);
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException(String.format("Unexpected number of parents for concept %s",
                    TITLE.getPropertyValue(conceptVertex)), iae);
        }
    }

    private Authorizations getAuthorizations() {
        return authorizations;
    }
}
