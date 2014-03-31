package com.altamiracorp.lumify.core.ingest.graphProperty;

import com.altamiracorp.bigtable.model.FlushFlag;
import com.altamiracorp.lumify.core.ingest.term.extraction.TermMention;
import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.LabelName;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
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
import java.util.Map;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.TITLE;
import static com.altamiracorp.lumify.core.util.CollectionUtil.trySingle;

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

    public abstract GraphPropertyWorkResult execute(InputStream in, GraphPropertyWorkData data) throws Exception;

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

    protected TermMentionModel saveTermMention(Vertex vertex, TermMention termMention, User user, Visibility visibility, Authorizations authorizations) {
        LOGGER.debug("Saving term mention '%s':%s (%d:%d)", termMention.getSign(), termMention.getOntologyClassUri(), termMention.getStart(), termMention.getEnd());
        TermMentionModel termMentionModel = new TermMentionModel(new TermMentionRowKey(vertex.getId().toString(), termMention.getStart(), termMention.getEnd()));
        termMentionModel.getMetadata().setSign(termMention.getSign(), visibility);
        termMentionModel.getMetadata().setOntologyClassUri(termMention.getOntologyClassUri(), visibility);
        if (termMention.getProcess() != null && !termMention.getProcess().equals("")) {
            termMentionModel.getMetadata().setAnalyticProcess(termMention.getProcess(), visibility);
        }

        Concept concept = ontologyRepository.getConceptById(termMention.getOntologyClassUri());
        if (concept == null) {
            LOGGER.error("Could not find ontology graph vertex '%s'", termMention.getOntologyClassUri());
            return null;
        }
        termMentionModel.getMetadata().setConceptGraphVertexId(concept.getId(), visibility);

        if (termMention.isResolved()) {
            String title = termMention.getSign();
            ElementMutation<Vertex> vertexElementMutation;
            if (termMention.getUseExisting()) {
                if (termMention.getId() != null) {
                    vertex = graph.getVertex(termMention.getId(), authorizations);
                } else {
                    vertex = trySingle(graph.query(authorizations)
                            .has(TITLE.getKey(), title)
                            .has(CONCEPT_TYPE.getKey(), concept.getId())
                            .vertices());
                }
            }
            if (vertex == null) {
                if (termMention.getId() != null) {
                    vertexElementMutation = graph.prepareVertex(termMention.getId(), visibility, authorizations);
                } else {
                    vertexElementMutation = graph.prepareVertex(visibility, authorizations);
                }
                TITLE.setProperty(vertexElementMutation, title, visibility);
                CONCEPT_TYPE.setProperty(vertexElementMutation, concept.getId(), visibility);
            } else {
                vertexElementMutation = vertex.prepareMutation();
            }

            if (termMention.getPropertyValue() != null) {
                Map<String, Object> properties = termMention.getPropertyValue();
                for (String key : properties.keySet()) {
                    // TODO should we wrap these properties in secure graph Text classes?
                    // GS - No.  Leave it up to the property generator to provide Text objects if they
                    // want index control; see CLAVIN for example
                    vertexElementMutation.setProperty(key, properties.get(key), visibility);
                }
            }

            if (!(vertexElementMutation instanceof ExistingElementMutation)) {
                vertex = vertexElementMutation.save();
                auditRepository.auditVertexElementMutation(AuditAction.UPDATE, vertexElementMutation, vertex, termMention.getProcess(), user, visibility);
            } else {
                auditRepository.auditVertexElementMutation(AuditAction.UPDATE, vertexElementMutation, vertex, termMention.getProcess(), user, visibility);
                vertex = vertexElementMutation.save();
            }

            // TODO: a better way to check if the same edge exists instead of looking it up every time?
            Edge edge = trySingle(vertex.getEdges(vertex, Direction.OUT, LabelName.RAW_HAS_ENTITY.toString(), authorizations));
            if (edge == null) {
                edge = graph.addEdge(vertex, vertex, LabelName.RAW_HAS_ENTITY.toString(), visibility, authorizations);
                auditRepository.auditRelationship(AuditAction.CREATE, vertex, vertex, edge, termMention.getProcess(), "", user, visibility);
            }

            termMentionModel.getMetadata().setVertexId(vertex.getId().toString(), visibility);
        }

        termMentionRepository.save(termMentionModel, FlushFlag.NO_FLUSH);
        return termMentionModel;
    }
}
