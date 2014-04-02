package com.altamiracorp.lumify.core.ingest.graphProperty;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.ingest.term.extraction.TermExtractionResult;
import com.altamiracorp.lumify.core.ingest.term.extraction.TermMention;
import com.altamiracorp.lumify.core.ingest.term.extraction.TermRelationship;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.mutation.ExistingElementMutation;
import com.google.inject.Inject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.altamiracorp.lumify.core.util.CollectionUtil.trySingle;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class GraphPropertyWorker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(GraphPropertyWorker.class);
    private Graph graph;
    private WorkQueueRepository workQueueRepository;
    private OntologyRepository ontologyRepository;
    private AuditRepository auditRepository;
    private TermMentionRepository termMentionRepository;
    private GraphPropertyWorkerPrepareData workerPrepareData;

    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        this.workerPrepareData = workerPrepareData;
    }

    public abstract void execute(InputStream in, GraphPropertyWorkData data) throws Exception;

    public abstract boolean isHandled(Vertex vertex, Property property);

    public boolean isLocalFileRequired() {
        return false;
    }

    protected User getUser() {
        return this.workerPrepareData.getUser();
    }

    public Authorizations getAuthorizations() {
        return this.workerPrepareData.getAuthorizations();
    }

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }

    protected Graph getGraph() {
        return graph;
    }

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    protected OntologyRepository getOntologyRepository() {
        return ontologyRepository;
    }

    @Inject
    public final void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    protected AuditRepository getAuditRepository() {
        return auditRepository;
    }

    @Inject
    public void setAuditRepository(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    protected TermMentionRepository getTermMentionRepository() {
        return termMentionRepository;
    }

    @Inject
    public void setTermMentionRepository(TermMentionRepository termMentionRepository) {
        this.termMentionRepository = termMentionRepository;
    }

    protected void saveTermExtractionResult(Vertex artifactGraphVertex, TermExtractionResult termExtractionResult) {
        List<TermMentionWithGraphVertex> termMentionsResults = saveTermMentions(artifactGraphVertex, termExtractionResult.getTermMentions());
        saveRelationships(termExtractionResult.getRelationships(), termMentionsResults);
    }

    private void saveRelationships(List<TermRelationship> relationships, List<TermMentionWithGraphVertex> termMentionsWithGraphVertices) {
        for (TermRelationship relationship : relationships) {
            TermMentionWithGraphVertex sourceTermMentionsWithGraphVertex = findTermMentionWithGraphVertex(termMentionsWithGraphVertices, relationship.getSourceTermMention());
            checkNotNull(sourceTermMentionsWithGraphVertex, "source was not found for " + relationship.getSourceTermMention());
            checkNotNull(sourceTermMentionsWithGraphVertex.getVertex(), "source vertex was not found for " + relationship.getSourceTermMention());
            TermMentionWithGraphVertex destTermMentionsWithGraphVertex = findTermMentionWithGraphVertex(termMentionsWithGraphVertices, relationship.getDestTermMention());
            checkNotNull(destTermMentionsWithGraphVertex, "dest was not found for " + relationship.getDestTermMention());
            checkNotNull(destTermMentionsWithGraphVertex.getVertex(), "dest vertex was not found for " + relationship.getDestTermMention());
            String label = relationship.getLabel();

            // TODO: a better way to check if the same edge exists instead of looking it up every time?
            Edge edge = trySingle(sourceTermMentionsWithGraphVertex.getVertex().getEdges(destTermMentionsWithGraphVertex.getVertex(), Direction.OUT, label, getAuthorizations()));
            if (edge == null) {
                graph.addEdge(
                        sourceTermMentionsWithGraphVertex.getVertex(),
                        destTermMentionsWithGraphVertex.getVertex(),
                        label,
                        relationship.getVisibility(),
                        getAuthorizations()
                );
            }
        }
        graph.flush();
    }

    private TermMentionWithGraphVertex findTermMentionWithGraphVertex(List<TermMentionWithGraphVertex> termMentionsWithGraphVertices, TermMention termMention) {
        for (TermMentionWithGraphVertex termMentionsWithGraphVertex : termMentionsWithGraphVertices) {
            if (termMentionsWithGraphVertex.getTermMention().getRowKey().getStartOffset() == termMention.getStart()
                    && termMentionsWithGraphVertex.getTermMention().getRowKey().getEndOffset() == termMention.getEnd()
                    && termMentionsWithGraphVertex.getVertex() != null) {
                return termMentionsWithGraphVertex;
            }
        }
        return null;
    }

    protected List<TermMentionWithGraphVertex> saveTermMentions(Vertex artifactGraphVertex, Iterable<TermMention> termMentions) {
        for (TermMentionFilter termMentionFilter : this.workerPrepareData.getTermMentionFilters()) {
            try {
                termMentions = termMentionFilter.apply(artifactGraphVertex, termMentions);
            } catch (Exception ex) {
                throw new LumifyException("Failed to run term mention filter: " + termMentionFilter.getClass().getName(), ex);
            }
        }

        List<TermMentionWithGraphVertex> results = new ArrayList<TermMentionWithGraphVertex>();
        for (TermMention termMention : termMentions) {
            results.add(saveTermMention(artifactGraphVertex, termMention));
        }
        return results;
    }

    private TermMentionWithGraphVertex saveTermMention(Vertex artifactGraphVertex, TermMention termMention) {
        LOGGER.debug("Saving term mention '%s':%s (%d:%d)", termMention.getSign(), termMention.getOntologyClassUri(), termMention.getStart(), termMention.getEnd());
        Vertex vertex = null;
        TermMentionModel termMentionModel = new TermMentionModel(new TermMentionRowKey(artifactGraphVertex.getId().toString(), termMention.getStart(), termMention.getEnd()));
        termMentionModel.getMetadata().setSign(termMention.getSign(), termMention.getVisibility());
        termMentionModel.getMetadata().setOntologyClassUri(termMention.getOntologyClassUri(), termMention.getVisibility());
        if (termMention.getProcess() != null && !termMention.getProcess().equals("")) {
            termMentionModel.getMetadata().setAnalyticProcess(termMention.getProcess(), termMention.getVisibility());
        }

        Concept concept = ontologyRepository.getConceptById(termMention.getOntologyClassUri());
        if (concept == null) {
            LOGGER.error("Could not find ontology graph vertex '%s'", termMention.getOntologyClassUri());
            return null;
        }
        termMentionModel.getMetadata().setConceptGraphVertexId(concept.getId(), termMention.getVisibility());

        if (termMention.isResolved()) {
            String title = termMention.getSign();
            ElementMutation<Vertex> vertexElementMutation;
            if (termMention.getUseExisting()) {
                if (termMention.getId() != null) {
                    vertex = graph.getVertex(termMention.getId(), getAuthorizations());
                } else {
                    vertex = trySingle(graph.query(getAuthorizations())
                            .has(LumifyProperties.TITLE.getKey(), title)
                            .has(OntologyLumifyProperties.CONCEPT_TYPE.getKey(), concept.getId())
                            .vertices());
                }
            }
            if (vertex == null) {
                if (termMention.getId() != null) {
                    vertexElementMutation = graph.prepareVertex(termMention.getId(), termMention.getVisibility(), getAuthorizations());
                } else {
                    vertexElementMutation = graph.prepareVertex(termMention.getVisibility(), getAuthorizations());
                }
                LumifyProperties.TITLE.setProperty(vertexElementMutation, title, termMention.getVisibility());
                OntologyLumifyProperties.CONCEPT_TYPE.setProperty(vertexElementMutation, concept.getId(), termMention.getVisibility());
            } else {
                vertexElementMutation = vertex.prepareMutation();
            }

            if (termMention.getPropertyValue() != null) {
                Map<String, Object> properties = termMention.getPropertyValue();
                for (String key : properties.keySet()) {
                    // TODO should we wrap these properties in secure graph Text classes?
                    // GS - No.  Leave it up to the property generator to provide Text objects if they
                    // want index control; see CLAVIN for example
                    vertexElementMutation.setProperty(key, properties.get(key), termMention.getVisibility());
                }
            }

            if (!(vertexElementMutation instanceof ExistingElementMutation)) {
                vertex = vertexElementMutation.save();
                auditRepository.auditVertexElementMutation(AuditAction.UPDATE, vertexElementMutation, vertex, termMention.getProcess(), getUser(), termMention.getVisibility());
            } else {
                auditRepository.auditVertexElementMutation(AuditAction.UPDATE, vertexElementMutation, vertex, termMention.getProcess(), getUser(), termMention.getVisibility());
                vertex = vertexElementMutation.save();
            }

            // TODO: a better way to check if the same edge exists instead of looking it up every time?
            Edge edge = trySingle(artifactGraphVertex.getEdges(vertex, Direction.OUT, LabelName.RAW_HAS_ENTITY.toString(), getAuthorizations()));
            if (edge == null) {
                edge = graph.addEdge(artifactGraphVertex, vertex, LabelName.RAW_HAS_ENTITY.toString(), termMention.getVisibility(), getAuthorizations());
                auditRepository.auditRelationship(AuditAction.CREATE, artifactGraphVertex, vertex, edge, termMention.getProcess(), "", getUser(), termMention.getVisibility());
            }

            termMentionModel.getMetadata().setVertexId(vertex.getId().toString(), termMention.getVisibility());
        }

        termMentionRepository.save(termMentionModel, FlushFlag.NO_FLUSH);
        return new TermMentionWithGraphVertex(termMentionModel, vertex);
    }

    public static class TermMentionWithGraphVertex {
        private final TermMentionModel termMention;
        private final Vertex vertex;

        public TermMentionWithGraphVertex(TermMentionModel termMention, Vertex vertex) {
            this.termMention = termMention;
            this.vertex = vertex;
        }

        public TermMentionModel getTermMention() {
            return termMention;
        }

        public Vertex getVertex() {
            return vertex;
        }
    }
}
