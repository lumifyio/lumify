package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.user.AuthorizationRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
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
import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class OntologyRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(OntologyRepository.class);
    public static final String VISIBILITY_STRING = "ontology";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);
    private final Graph graph;
    public static final String ROOT_CONCEPT_IRI = "http://lumify.io#root";
    public static final String ENTITY_CONCEPT_IRI = "http://www.w3.org/2002/07/owl#Thing";
    public static final String TYPE_RELATIONSHIP = "relationship";
    public static final String TYPE_CONCEPT = "concept";
    public static final String TYPE_PROPERTY = "property";
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
    public OntologyRepository(
            Graph graph,
            AuthorizationRepository authorizationRepository) {
        this.graph = graph;

        authorizationRepository.addAuthorizationToGraph(OntologyRepository.VISIBILITY_STRING);

        Set<String> authorizationsSet = new HashSet<String>();
        authorizationsSet.add(VISIBILITY_STRING);
        this.authorizations = authorizationRepository.createAuthorizations(authorizationsSet);
    }

    public void clearCache() {
        LOGGER.info("clearing ontology cache");
        this.allConceptsWithPropertiesCache.invalidateAll();
        this.allPropertiesCache.invalidateAll();
        this.conceptsCache.invalidateAll();
        this.relationshipLabelsCache.invalidateAll();
    }

    public void storeOntologyFile(File file, IRI documentIRI) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            storeOntologyFile(in, documentIRI);
        } finally {
            in.close();
        }
    }

    public void storeOntologyFile(InputStream in, IRI documentIRI) {
        StreamingPropertyValue value = new StreamingPropertyValue(in, byte[].class);
        value.searchIndex(false);
        Map<String, Object> metadata = new HashMap<String, Object>();
        Vertex rootConceptVertex = getRootConcept().getVertex();
        metadata.put("index", toList(rootConceptVertex.getProperties("ontologyFile")).size());
        rootConceptVertex.addPropertyValue(documentIRI.toString(), "ontologyFile", value, metadata, VISIBILITY.getVisibility());
        graph.flush();
    }

    public Iterable<Property> getOntologyFiles() {
        List<Property> ontologyFiles = toList(getRootConcept().getVertex().getProperties("ontologyFile"));
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
                            return new Relationship(vertex, sourceVertexId, destVertexId);
                        }
                    });
                }
            });
        } catch (ExecutionException e) {
            throw new LumifyException("Could not get relationship labels");
        }
    }

    public String getDisplayNameForLabel(String relationshipLabel) {
        String displayName = null;
        if (relationshipLabel != null && !relationshipLabel.trim().isEmpty()) {
            try {
                Vertex relVertex = Iterables.getOnlyElement(graph.query(getAuthorizations())
                        .has(CONCEPT_TYPE.getKey(), TYPE_RELATIONSHIP)
                        .has(ONTOLOGY_TITLE.getKey(), relationshipLabel)
                        .vertices(), null);
                displayName = relVertex != null ? DISPLAY_NAME.getPropertyValue(relVertex) : null;
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException(String.format("Found multiple vertices for relationship label \"%s\"", relationshipLabel),
                        iae);
            }
        }
        return displayName;
    }

    public List<OntologyProperty> getProperties() {
        try {
            return allPropertiesCache.get("", new TimingCallable<List<OntologyProperty>>("getProperties") {
                @Override
                public List<OntologyProperty> callWithTime() throws Exception {
                    return toList(new ConvertingIterable<Vertex, OntologyProperty>(graph.query(getAuthorizations())
                            .has(CONCEPT_TYPE.getKey(), TYPE_PROPERTY)
                            .vertices()) {
                        @Override
                        protected OntologyProperty convert(Vertex vertex) {
                            return new OntologyProperty(vertex);
                        }
                    });
                }
            });
        } catch (ExecutionException e) {
            throw new LumifyException("Could not get properties", e);
        }
    }

    public OntologyProperty getProperty(String propertyName) {
        try {
            Vertex propVertex = Iterables.getOnlyElement(graph.query(getAuthorizations())
                    .has(CONCEPT_TYPE.getKey(), TYPE_PROPERTY)
                    .has(ONTOLOGY_TITLE.getKey(), propertyName)
                    .vertices(), null);
            return propVertex != null ? new OntologyProperty(propVertex) : null;
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException(String.format("Too many \"%s\" properties", propertyName), iae);
        }
    }

    public Relationship getRelationshipById(String relationshipId) {
        Vertex relationshipVertex = graph.getVertex(relationshipId, getAuthorizations());
        if (relationshipVertex == null) {
            return null;
        }
        String from = single(relationshipVertex.getVertexIds(Direction.IN, getAuthorizations())).toString();
        String to = single(relationshipVertex.getVertexIds(Direction.OUT, getAuthorizations())).toString();
        return new Relationship(relationshipVertex, from, to);
    }

    public Iterable<Concept> getConcepts() {
        return getConcepts(false);
    }

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
                    return new Concept(vertex, parentConceptVertex, conceptProperties);
                } else {
                    return new Concept(vertex);
                }
            }
        };
    }

    public Concept getRootConcept() {
        return getConceptById(OntologyRepository.ROOT_CONCEPT_IRI);
    }

    public Concept getEntityConcept() {
        return getConceptById(OntologyRepository.ENTITY_CONCEPT_IRI);
    }

    public List<Concept> getChildConcepts(Concept concept) {
        Vertex conceptVertex = graph.getVertex(concept.getId(), getAuthorizations());
        return toConcepts(conceptVertex.getVertices(Direction.IN, LabelName.IS_A.toString(), getAuthorizations()));
    }

    public Concept getParentConcept(final Concept concept) {
        return getParentConcept(concept.getId());
    }

    public Concept getParentConcept(String conceptId) {
        Vertex conceptVertex = graph.getVertex(conceptId, getAuthorizations());
        Vertex parentConceptVertex = getParentConceptVertex(conceptVertex);
        if (parentConceptVertex == null) {
            return null;
        }
        return new Concept(parentConceptVertex);
    }

    private List<Concept> toConcepts(Iterable<Vertex> vertices) {
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        for (Vertex vertex : vertices) {
            concepts.add(new Concept(vertex));
        }
        return concepts;
    }

    public Concept getConceptById(String conceptVertexId) {
        Vertex conceptVertex = graph.getVertex(conceptVertexId, getAuthorizations());
        return conceptVertex != null ? new Concept(conceptVertex) : null;
    }

    private List<OntologyProperty> getPropertiesByVertex(Vertex vertex) {
        List<OntologyProperty> properties = new ArrayList<OntologyProperty>();

        properties.addAll(getPropertiesByVertexNoRecursion(vertex));

        Vertex parentConceptVertex = getParentConceptVertex(vertex);
        if (parentConceptVertex != null) {
            List<OntologyProperty> parentProperties = getPropertiesByVertex(parentConceptVertex);
            properties.addAll(parentProperties);
        }

        return properties;
    }

    public List<OntologyProperty> getPropertiesByConceptIdNoRecursion(String conceptVertexId) {
        Vertex conceptVertex = graph.getVertex(conceptVertexId, getAuthorizations());
        if (conceptVertex == null) {
            throw new RuntimeException("Could not find concept: " + conceptVertexId);
        }
        return getPropertiesByVertexNoRecursion(conceptVertex);
    }

    private List<OntologyProperty> getPropertiesByVertexNoRecursion(Vertex vertex) {
        return toList(new ConvertingIterable<Vertex, OntologyProperty>(vertex.getVertices(Direction.OUT, LabelName.HAS_PROPERTY.toString(), getAuthorizations())) {
            @Override
            protected OntologyProperty convert(Vertex o) {
                return new OntologyProperty(o);
            }
        });
    }

    public OntologyProperty getPropertyById(String propertyId) {
        List<OntologyProperty> properties = getProperties();
        for (OntologyProperty property : properties) {
            if (property.getId().equals(propertyId)) {
                return property;
            }
        }
        return null;
    }

    public List<Concept> getConceptByIdAndChildren(String conceptId) {
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        Concept concept = getConceptById(conceptId);
        if (concept == null) {
            return null;
        }
        concepts.add(concept);
        List<Concept> children = getChildConcepts(concept);
        concepts.addAll(children);
        return concepts;
    }

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

    public List<OntologyProperty> getPropertiesByRelationship(Relationship relationship) {
        return getPropertiesByVertex(relationship.getVertex());
    }

    public Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName) {
        Concept concept = getConceptById(conceptIRI);
        if (concept != null) {
            return concept;
        }

        VertexBuilder builder = graph.prepareVertex(conceptIRI, VISIBILITY.getVisibility(), getAuthorizations());
        CONCEPT_TYPE.setProperty(builder, TYPE_CONCEPT, VISIBILITY.getVisibility());
        ONTOLOGY_TITLE.setProperty(builder, conceptIRI, VISIBILITY.getVisibility());
        DISPLAY_NAME.setProperty(builder, displayName, VISIBILITY.getVisibility());
        Vertex vertex = builder.save();

        concept = new Concept(vertex);
        if (parent != null) {
            findOrAddEdge(concept.getVertex(), parent.getVertex(), LabelName.IS_A.toString());
        }

        graph.flush();
        return concept;
    }

    protected void findOrAddEdge(Vertex fromVertex, final Vertex toVertex, String edgeLabel) {
        Iterator<Vertex> matchingEdges = new FilterIterable<Vertex>(fromVertex.getVertices(Direction.OUT, edgeLabel, getAuthorizations())) {
            @Override
            protected boolean isIncluded(Vertex vertex) {
                return vertex.getId().equals(toVertex.getId());
            }
        }.iterator();
        if (matchingEdges.hasNext()) {
            return;
        }
        fromVertex.getGraph().addEdge(fromVertex, toVertex, edgeLabel, VISIBILITY.getVisibility(), getAuthorizations());
    }

    public OntologyProperty addPropertyTo(Vertex vertex, String propertyId, String displayName, PropertyType dataType, boolean userVisible) {
        checkNotNull(vertex, "vertex was null");
        OntologyProperty property = getOrCreatePropertyType(propertyId, dataType, displayName, userVisible);
        checkNotNull(property, "Could not find property: " + propertyId);

        findOrAddEdge(vertex, property.getVertex(), LabelName.HAS_PROPERTY.toString());

        graph.flush();
        return property;
    }

    public Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipId, String displayName) {
        Relationship relationship = getRelationshipById(relationshipId);
        if (relationship != null) {
            return relationship;
        }

        VertexBuilder builder = graph.prepareVertex(relationshipId, VISIBILITY.getVisibility(), getAuthorizations());
        CONCEPT_TYPE.setProperty(builder, TYPE_RELATIONSHIP, VISIBILITY.getVisibility());
        ONTOLOGY_TITLE.setProperty(builder, relationshipId, VISIBILITY.getVisibility());
        DISPLAY_NAME.setProperty(builder, displayName, VISIBILITY.getVisibility());
        Vertex relationshipVertex = builder.save();

        findOrAddEdge(from.getVertex(), relationshipVertex, LabelName.HAS_EDGE.toString());
        findOrAddEdge(relationshipVertex, to.getVertex(), LabelName.HAS_EDGE.toString());

        graph.flush();
        return new Relationship(relationshipVertex, from.getId(), to.getId());
    }

    public void resolvePropertyIds(JSONArray filterJson) throws JSONException {
        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject filter = filterJson.getJSONObject(i);
            if (filter.has("propertyId") && !filter.has("propertyName")) {
                String propertyId = filter.getString("propertyId");
                OntologyProperty property = getPropertyById(propertyId);
                if (property == null) {
                    throw new RuntimeException("Could not find property with id: " + propertyId);
                }
                filter.put("propertyName", property.getTitle());
                filter.put("propertyDataType", property.getDataType());
            }
        }
    }

    public OntologyProperty getOrCreatePropertyType(final String propertyName, final PropertyType dataType, final String displayName, boolean userVisible) {
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
            typeProperty = new OntologyProperty(builder.save());
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

    public Authorizations getAuthorizations() {
        return authorizations;
    }
}
