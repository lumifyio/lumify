package com.altamiracorp.lumify.core.ontology;

import com.altamiracorp.lumify.core.model.GraphSession;
import com.altamiracorp.lumify.core.model.artifact.ArtifactType;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.*;
import com.altamiracorp.lumify.core.model.resources.ResourceRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.inject.Inject;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.core.TitanLabel;
import com.thinkaurelius.titan.core.TypeMaker;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

import java.io.InputStream;

import static org.mockito.internal.util.Checks.checkNotNull;

public class BaseOntology {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BaseOntology.class);

    private final OntologyRepository ontologyRepository;
    private final ResourceRepository resourceRepository;
    private final GraphSession graphSession;

    @Inject
    public BaseOntology(OntologyRepository ontologyRepository, ResourceRepository resourceRepository, GraphSession graphSession) {
        this.ontologyRepository = ontologyRepository;
        this.resourceRepository = resourceRepository;
        this.graphSession = graphSession;
    }

    public void defineOntology(User user) {
        // concept properties
        TitanGraph graph = (TitanGraph) this.graphSession.getGraph();

        TitanKey typeProperty = (TitanKey) graph.getType(PropertyName.CONCEPT_TYPE.toString());
        if (typeProperty == null) {
            typeProperty = graph.makeType().name(PropertyName.CONCEPT_TYPE.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed(Vertex.class).makePropertyKey();
        }

        TitanKey dataTypeProperty = (TitanKey) graph.getType(PropertyName.DATA_TYPE.toString());
        if (dataTypeProperty == null) {
            graph.makeType().name(PropertyName.DATA_TYPE.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey ontologyTitleProperty = (TitanKey) graph.getType(PropertyName.ONTOLOGY_TITLE.toString());
        if (ontologyTitleProperty == null) {
            graph.makeType().name(PropertyName.ONTOLOGY_TITLE.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed(Vertex.class).makePropertyKey();
        }

        // concept edges
        TitanLabel hasPropertyEdge = (TitanLabel) graph.getType(LabelName.HAS_PROPERTY.toString());
        if (hasPropertyEdge == null) {
            graph.makeType().name(LabelName.HAS_PROPERTY.toString()).directed().makeEdgeLabel();
        }

        TitanLabel hasEdgeEdge = (TitanLabel) graph.getType(LabelName.HAS_EDGE.toString());
        if (hasEdgeEdge == null) {
            graph.makeType().name(LabelName.HAS_EDGE.toString()).directed().makeEdgeLabel();
        }

        TitanLabel isAEdge = (TitanLabel) graph.getType(LabelName.IS_A.toString());
        if (isAEdge == null) {
            graph.makeType().name(LabelName.IS_A.toString()).directed().makeEdgeLabel();
        }

        TitanKey relationshipType = (TitanKey) graph.getType(PropertyName.RELATIONSHIP_TYPE.toString());
        if (relationshipType == null) {
            graph.makeType().name(PropertyName.RELATIONSHIP_TYPE.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed(Vertex.class).makePropertyKey();
        }

        TitanKey timeStampProperty = (TitanKey) graph.getType(PropertyName.TIME_STAMP.toString());
        if (timeStampProperty == null) {
            graph.makeType().name(PropertyName.TIME_STAMP.toString()).dataType(Long.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey rawHdfsPath = (TitanKey) graph.getType(PropertyName.RAW_HDFS_PATH.toString());
        if (rawHdfsPath == null) {
            graph.makeType().name(PropertyName.RAW_HDFS_PATH.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey textHdfsPath = (TitanKey) graph.getType(PropertyName.TEXT_HDFS_PATH.toString());
        if (textHdfsPath == null) {
            graph.makeType().name(PropertyName.TEXT_HDFS_PATH.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey highlightedTextHdfsPath = (TitanKey) graph.getType(PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH.toString());
        if (highlightedTextHdfsPath == null) {
            graph.makeType().name(PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey detectedObjects = (TitanKey) graph.getType(PropertyName.DETECTED_OBJECTS.toString());
        if (detectedObjects == null) {
            graph.makeType().name(PropertyName.DETECTED_OBJECTS.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey displayNameProperty = (TitanKey) graph.getType(PropertyName.DISPLAY_NAME.toString());
        if (displayNameProperty == null) {
            graph.makeType().name(PropertyName.DISPLAY_NAME.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey displayTypeProperty = (TitanKey) graph.getType(PropertyName.DISPLAY_TYPE.toString());
        if (displayTypeProperty == null) {
            graph.makeType().name(PropertyName.DISPLAY_TYPE.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey titleProperty = (TitanKey) graph.getType(PropertyName.TITLE.toString());
        if (titleProperty == null) {
            titleProperty = graph.makeType().name(PropertyName.TITLE.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed("search", Vertex.class).makePropertyKey();
        }

        TitanKey glyphIconProperty = (TitanKey) graph.getType(PropertyName.GLYPH_ICON.toString());
        if (glyphIconProperty == null) {
            graph.makeType().name(PropertyName.GLYPH_ICON.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey mapGlyphIconProperty = (TitanKey) graph.getType(PropertyName.MAP_GLYPH_ICON.toString());
        if (mapGlyphIconProperty == null) {
            graph.makeType().name(PropertyName.MAP_GLYPH_ICON.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey colorProperty = (TitanKey) graph.getType(PropertyName.COLOR.toString());
        if (colorProperty == null) {
            graph.makeType().name(PropertyName.COLOR.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).makePropertyKey();
        }

        TitanKey geoLocationProperty = (TitanKey) graph.getType(PropertyName.GEO_LOCATION.toString());
        if (geoLocationProperty == null) {
            graph.makeType().name(PropertyName.GEO_LOCATION.toString()).dataType(Geoshape.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed("search", Vertex.class).makePropertyKey();
        }

        TitanKey geoLocationDescriptionProperty = (TitanKey) graph.getType(PropertyName.GEO_LOCATION_DESCRIPTION.toString());
        if (geoLocationDescriptionProperty == null) {
            graph.makeType().name(PropertyName.GEO_LOCATION_DESCRIPTION.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed("search", Vertex.class).makePropertyKey();
        }

        TitanKey publishedDateProperty = (TitanKey) graph.getType(PropertyName.PUBLISHED_DATE.toString());
        if (publishedDateProperty == null) {
            graph.makeType().name(PropertyName.PUBLISHED_DATE.toString()).dataType(Long.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed("search", Vertex.class).makePropertyKey();
        }

        TitanKey sourceProperty = (TitanKey) graph.getType(PropertyName.SOURCE.toString());
        if (sourceProperty == null) {
            graph.makeType().name(PropertyName.SOURCE.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed("search", Vertex.class).makePropertyKey();
        }


        TitanKey authorProperty = (TitanKey) graph.getType(PropertyName.AUTHOR.toString());
        if (authorProperty == null) {
            graph.makeType().name(PropertyName.AUTHOR.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed("search", Vertex.class).makePropertyKey();
        }
        graph.commit();

        String process = this.getClass().getName();

        Concept rootConcept = ontologyRepository.getOrCreateConcept(null, OntologyRepository.ROOT_CONCEPT_NAME, process, OntologyRepository.ROOT_CONCEPT_NAME, user);
        ontologyRepository.addPropertyTo(rootConcept, PropertyName.GLYPH_ICON.toString(), "glyph icon", process, PropertyType.IMAGE, user);
        ontologyRepository.addPropertyTo(rootConcept, PropertyName.MAP_GLYPH_ICON.toString(), "map glyph icon", process, PropertyType.IMAGE, user);
        graph.commit();

        // TermMention concept
        TitanKey rowKeyProperty = (TitanKey) graph.getType(PropertyName.ROW_KEY.toString());
        if (rowKeyProperty == null) {
            graph.makeType().name(PropertyName.ROW_KEY.toString()).dataType(String.class).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed(Vertex.class).makePropertyKey();
        }

        graph.commit();

        // Entity concept
        Concept entity = ontologyRepository.getOrCreateConcept(rootConcept, VertexType.ENTITY.toString(), process, "Entity", user);
        ontologyRepository.addPropertyTo(entity, typeProperty.getName(), "Type", process, PropertyType.STRING, user);
        ontologyRepository.addPropertyTo(entity, titleProperty.getName(), "Title", process, PropertyType.STRING, user);

        graph.commit();

        InputStream entityGlyphIconInputStream = this.getClass().getResourceAsStream("entity.png");
        String entityGlyphIconRowKey = resourceRepository.importFile(entityGlyphIconInputStream, "png", user);
        entity.setProperty(PropertyName.GLYPH_ICON, entityGlyphIconRowKey);
        graph.commit();
    }

    public boolean isOntologyDefined(User user) {
        try {
            Concept concept = ontologyRepository.getConceptByName(VertexType.ENTITY.toString(), user);
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
