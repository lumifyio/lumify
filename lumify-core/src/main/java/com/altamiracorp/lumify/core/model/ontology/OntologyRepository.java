package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.user.AuthorizationBuilder;
import com.altamiracorp.securegraph.*;
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

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.*;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.*;
import static com.altamiracorp.lumify.core.util.CollectionUtil.single;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class OntologyRepository {
    public static final String VISIBILITY_STRING = "ontology";
    public static final Visibility DEFAULT_VISIBILITY = new Visibility(VISIBILITY_STRING);
    private final Graph graph;
    public static final String ROOT_CONCEPT_NAME = "rootConcept";
    public static final String TYPE_RELATIONSHIP = "relationship";
    public static final String TYPE_CONCEPT = "concept";
    public static final String TYPE_PROPERTY = "property";
    public static final String TYPE_ENTITY = "entity";
    private Authorizations authorizations;
    private Cache<String, Concept> conceptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @Inject
    public OntologyRepository(Graph graph, AuthorizationBuilder authorizationBuilder) {
        this.graph = graph;
        Set<String> authorizationsSet = new HashSet<String>();
        authorizationsSet.add(VISIBILITY_STRING);
        this.authorizations = authorizationBuilder.create(authorizationsSet);
    }

    public Iterable<Relationship> getRelationshipLabels() {
        Iterable<Vertex> vertices = graph.query(getAuthorizations())
                .has(DISPLAY_TYPE.getKey(), TYPE_RELATIONSHIP)
                .limit(10000)
                .vertices();

        return new ConvertingIterable<Vertex, Relationship>(vertices) {
            @Override
            protected Relationship convert(Vertex vertex) {
                Vertex sourceVertex = single(vertex.getVertices(Direction.IN, getAuthorizations()));
                Vertex destVertex = single(vertex.getVertices(Direction.OUT, getAuthorizations()));
                return new Relationship(vertex, new Concept(sourceVertex), new Concept(destVertex));
            }
        };
    }

    public Iterable<Relationship> getRelationships(String sourceConceptTypeId, String destConceptTypeId) {
        Concept sourceConcept = getConceptById(sourceConceptTypeId);
        if (sourceConcept == null) {
            sourceConcept = getConceptByName(sourceConceptTypeId);
            if (sourceConcept == null) {
                throw new RuntimeException("Could not find concept: " + sourceConceptTypeId);
            }
        }
        Concept destConcept = getConceptById(destConceptTypeId);
        if (destConcept == null) {
            destConcept = getConceptByName(destConceptTypeId);
            if (destConcept == null) {
                throw new RuntimeException("Could not find concept: " + destConceptTypeId);
            }
        }

        return getRelationships(sourceConcept, destConcept);
    }

    private Iterable<Relationship> getRelationships(final Concept sourceConcept, final Concept destConcept) {
        final List<Vertex> sourceAndParents = getConceptParents(sourceConcept);
        final List<Vertex> destAndParents = getConceptParents(destConcept);

        return new FilterIterable<Relationship>(getRelationshipLabels()) {
            @Override
            protected boolean isIncluded(Relationship relationship) {
                Object sourceId = relationship.getSourceConcept().getId();
                Object destId = relationship.getDestConcept().getId();
                for (Vertex s : sourceAndParents) {
                    if (!s.getId().equals(sourceId)) {
                        continue;
                    }
                    for (Vertex d : destAndParents) {
                        if (d.getId().equals(destId)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    public String getDisplayNameForLabel(String relationshipLabel) {
        String displayName = null;
        if (relationshipLabel != null && !relationshipLabel.trim().isEmpty()) {
            try {
                Vertex relVertex = Iterables.getOnlyElement(graph.query(getAuthorizations())
                        .has(DISPLAY_TYPE.getKey(), TYPE_RELATIONSHIP)
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
        List<OntologyProperty> properties = new ArrayList<OntologyProperty>();
        Iterable<Vertex> vertices = graph.query(getAuthorizations())
                .has(DISPLAY_TYPE.getKey(), TYPE_PROPERTY)
                .vertices();
        for (Vertex vertex : vertices) {
            properties.add(new OntologyProperty(vertex));
        }
        return properties;
    }

    public OntologyProperty getProperty(String propertyName) {
        try {
            Vertex propVertex = Iterables.getOnlyElement(graph.query(getAuthorizations())
                    .has(DISPLAY_TYPE.getKey(), TYPE_PROPERTY)
                    .has(ONTOLOGY_TITLE.getKey(), propertyName)
                    .vertices(), null);
            return propVertex != null ? new OntologyProperty(propVertex) : null;
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException(String.format("Too many \"%s\" properties", propertyName), iae);
        }
    }

    public Relationship getRelationship(String propertyName) {
        try {
            Vertex relVertex = Iterables.getOnlyElement(graph.query(getAuthorizations())
                    .has(DISPLAY_TYPE.getKey(), TYPE_RELATIONSHIP)
                    .has(ONTOLOGY_TITLE.getKey(), propertyName)
                    .vertices(), null);
            Relationship relationship = null;
            if (relVertex != null) {
                Concept from = getConceptById(relVertex.getVertices(Direction.IN, getAuthorizations()).iterator().next().getId());
                Concept to = getConceptById(relVertex.getVertices(Direction.OUT, getAuthorizations()).iterator().next().getId());
                relationship = new Relationship(relVertex, from, to);
            }
            return relationship;
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException(String.format("Too many \"%s\" relationshipVertices.", propertyName), iae);
        }
    }

    public Concept getRootConcept() {
        try {
            Vertex rootVertex = Iterables.getOnlyElement(graph.query(getAuthorizations())
                    .has(CONCEPT_TYPE.getKey(), TYPE_CONCEPT)
                    .has(ONTOLOGY_TITLE.getKey(), OntologyRepository.ROOT_CONCEPT_NAME)
                    .vertices());
            return new Concept(rootVertex);
        } catch (NoSuchElementException nsee) {
            throw new IllegalStateException(String.format("Could not find \"%s\" concept.", ROOT_CONCEPT_NAME), nsee);
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException(String.format("Too many \"%s\" concepts.", ROOT_CONCEPT_NAME), iae);
        }
    }

    public List<Concept> getChildConcepts(Concept concept) {
        Vertex conceptVertex = graph.getVertex(concept.getId(), getAuthorizations());
        return toConcepts(conceptVertex.getVertices(Direction.IN, LabelName.IS_A.toString(), getAuthorizations()));
    }

    public Concept getParentConcept(final Concept concept) {
        return getParentConcept(concept.getId().toString());
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

    public Concept getConceptById(Object conceptVertexId) {
        Vertex conceptVertex = graph.getVertex(conceptVertexId, getAuthorizations());
        return conceptVertex != null ? new Concept(conceptVertex) : null;
    }

    public Concept getConceptByName(String title) {
        Concept concept = conceptsCache.getIfPresent(title);
        if (concept != null) {
            return concept;
        }

        Vertex vertex = findOntologyConceptByTitle(title);
        if (vertex == null) {
            return null;
        }
        concept = new Concept(vertex);
        conceptsCache.put(title, concept);
        return concept;
    }

    public Vertex getGraphVertexByTitle(String title) {
        return findVertexByOntologyTitle(title);
    }

    public List<OntologyProperty> getPropertiesByConceptId(String conceptVertexId) {
        Concept conceptVertex = getConceptById(conceptVertexId);
        if (conceptVertex == null) {
            conceptVertex = getConceptByName(conceptVertexId);
            if (conceptVertex == null) {
                throw new NoSuchElementException("Could not find concept: " + conceptVertexId);
            }
        }
        return getPropertiesByVertex(conceptVertex.getVertex());
    }

    private List<OntologyProperty> getPropertiesByVertex(Vertex vertex) {
        List<OntologyProperty> properties = new ArrayList<OntologyProperty>();

        Iterable<Vertex> propertyVertices = vertex.getVertices(Direction.OUT, LabelName.HAS_PROPERTY.toString(), getAuthorizations());
        for (Vertex propertyVertex : propertyVertices) {
            properties.add(new OntologyProperty(propertyVertex));
        }

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
        List<OntologyProperty> properties = new ArrayList<OntologyProperty>();

        Iterable<Vertex> propertyVertices = vertex.getVertices(Direction.OUT, LabelName.HAS_PROPERTY.toString(), getAuthorizations());
        for (Vertex propertyVertex : propertyVertices) {
            properties.add(new OntologyProperty(propertyVertex));
        }

        return properties;
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

    public List<OntologyProperty> getPropertiesByRelationship(String relationshipLabel) {
        Vertex relationshipVertex = getRelationshipVertexId(relationshipLabel);
        if (relationshipVertex == null) {
            throw new RuntimeException("Could not find relationship: " + relationshipLabel);
        }
        return getPropertiesByVertex(relationshipVertex);
    }

    private Vertex getRelationshipVertexId(String relationshipLabel) {
        try {
            return Iterables.getOnlyElement(graph.query(getAuthorizations())
                    .has(DISPLAY_TYPE.getKey(), TYPE_RELATIONSHIP)
                    .has(ONTOLOGY_TITLE.getKey(), relationshipLabel)
                    .vertices());
        } catch (NoSuchElementException nsee) {
            throw new IllegalStateException(String.format("Could not find \"%s\" vertex", relationshipLabel), nsee);
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException(String.format("Too many \"%s\" vertices", relationshipLabel), iae);
        }
    }

    public Concept getOrCreateConcept(Concept parent, String conceptName, String displayName) {
        Concept concept = getConceptByName(conceptName);
        if (concept != null) {
            return concept;
        }

        VertexBuilder builder = graph.prepareVertex(DEFAULT_VISIBILITY, getAuthorizations());
        CONCEPT_TYPE.setProperty(builder, TYPE_CONCEPT, DEFAULT_VISIBILITY);
        ONTOLOGY_TITLE.setProperty(builder, conceptName, DEFAULT_VISIBILITY);
        DISPLAY_NAME.setProperty(builder, displayName, DEFAULT_VISIBILITY);
        Vertex vertex = builder.save();

        concept = new Concept(vertex);
        if (parent != null) {
            findOrAddEdge(concept.getVertex(), parent.getVertex(), LabelName.IS_A.toString());
        }

        graph.flush();
        return concept;
    }

    protected void findOrAddEdge(Vertex fromVertex, final Vertex toVertex, String edgeLabel) {
        Iterator<Vertex> matchingEdges = new FilterIterable<Vertex>(fromVertex.getVertices(Direction.BOTH, edgeLabel, getAuthorizations())) {
            @Override
            protected boolean isIncluded(Vertex vertex) {
                return vertex.getId().equals(toVertex.getId());
            }
        }.iterator();
        if (matchingEdges.hasNext()) {
            return;
        }
        fromVertex.getGraph().addEdge(fromVertex, toVertex, edgeLabel, DEFAULT_VISIBILITY, getAuthorizations());
    }

    public OntologyProperty addPropertyTo(Vertex vertex, String propertyName, String displayName, PropertyType dataType) {
        checkNotNull(vertex, "vertex was null");
        OntologyProperty property = getOrCreatePropertyType(propertyName, dataType, displayName);
        checkNotNull(property, "Could not find property: " + propertyName);

        findOrAddEdge(vertex, property.getVertex(), LabelName.HAS_PROPERTY.toString());

        graph.flush();
        return property;
    }

    public Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipName, String displayName) {
        Relationship relationship = getRelationship(relationshipName);
        if (relationship != null) {
            return relationship;
        }

        VertexBuilder builder = graph.prepareVertex(DEFAULT_VISIBILITY, getAuthorizations());
        CONCEPT_TYPE.setProperty(builder, TYPE_CONCEPT, DEFAULT_VISIBILITY);
        ONTOLOGY_TITLE.setProperty(builder, relationshipName, DEFAULT_VISIBILITY);
        DISPLAY_NAME.setProperty(builder, displayName, DEFAULT_VISIBILITY);
        DISPLAY_TYPE.setProperty(builder, TYPE_RELATIONSHIP, DEFAULT_VISIBILITY);
        Vertex relationshipVertex = builder.save();

        findOrAddEdge(from.getVertex(), relationshipVertex, LabelName.HAS_EDGE.toString());
        findOrAddEdge(relationshipVertex, to.getVertex(), LabelName.HAS_EDGE.toString());

        graph.flush();
        return new Relationship(relationshipVertex, from, to);
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

    public OntologyProperty getOrCreatePropertyType(final String propertyName, final PropertyType dataType, final String displayName) {
        OntologyProperty typeProperty = getProperty(propertyName);
        if (typeProperty == null) {
            VertexBuilder builder = graph.prepareVertex(DEFAULT_VISIBILITY, getAuthorizations());
            CONCEPT_TYPE.setProperty(builder, TYPE_PROPERTY, DEFAULT_VISIBILITY);
            DISPLAY_TYPE.setProperty(builder, TYPE_PROPERTY, DEFAULT_VISIBILITY);
            ONTOLOGY_TITLE.setProperty(builder, propertyName, DEFAULT_VISIBILITY);
            DATA_TYPE.setProperty(builder, dataType.toString(), DEFAULT_VISIBILITY);
            if (displayName != null && !displayName.trim().isEmpty()) {
                DISPLAY_NAME.setProperty(builder, displayName.trim(), DEFAULT_VISIBILITY);
            }
            typeProperty = new OntologyProperty(builder.save());
            graph.flush();
        }
        return typeProperty;
    }

    private List<Vertex> getConceptParents(Concept concept) {
        ArrayList<Vertex> results = new ArrayList<Vertex>();
        results.add(concept.getVertex());
        Vertex v = concept.getVertex();
        while ((v = getParentConceptVertex(v)) != null) {
            results.add(v);
        }
        return results;
    }

    private Vertex getParentConceptVertex(Vertex conceptVertex) {
        try {
            return Iterables.getOnlyElement(conceptVertex.getVertices(Direction.OUT, LabelName.IS_A.toString(), getAuthorizations()), null);
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException(String.format("Unexpected number of parents for concept %s",
                    TITLE.getPropertyValue(conceptVertex)), iae);
        }
    }

    private Vertex findOntologyConceptByTitle(String title) {
        return Iterables.getFirst(graph.query(getAuthorizations())
                .has(ONTOLOGY_TITLE.getKey(), title)
                .has(CONCEPT_TYPE.getKey(), OntologyRepository.TYPE_CONCEPT)
                .vertices(), null);
    }

    private Vertex findVertexByOntologyTitle(String title) {
        return Iterables.getFirst(graph.query(getAuthorizations())
                .has(ONTOLOGY_TITLE.getKey(), title)
                .vertices(), null);
    }

    public Authorizations getAuthorizations() {
        return authorizations;
    }
}
