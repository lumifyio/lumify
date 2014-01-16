package com.altamiracorp.lumify.storm.textHighlighting;

import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.storm.BaseTextProcessingBolt;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONObject;

public class ArtifactHighlightingBolt extends BaseTextProcessingBolt {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactHighlightingBolt.class);
    private TermMentionRepository termMentionRepository;
    private EntityHighlighter entityHighlighter;

    @Override
    protected void safeExecute(Tuple input) throws Exception {
        final JSONObject json = getJsonFromTuple(input);
        final String graphVertexId = json.getString("graphVertexId");

        Vertex graphVertex = graph.getVertex(graphVertexId, getUser().getAuthorizations());
        if (graphVertex != null) {
            String artifactRowKey = (String) graphVertex.getPropertyValue(PropertyName.ROW_KEY.toString(), 0);
            LOGGER.debug("Processing graph vertex [%s] for artifact: %s", graphVertex.getId(), artifactRowKey);

            Iterable<TermMentionModel> termMentions = termMentionRepository.findByGraphVertexId(graphVertex.getId().toString(), getUser());
            performHighlighting(artifactRowKey, graphVertex, termMentions);
        } else {
            LOGGER.warn("Could not find vertex with id: %s", graphVertexId);
        }
    }

    private void performHighlighting(final String rowKey, final Vertex vertex, final Iterable<TermMentionModel> termMentions) throws Exception {
        String text = getText(vertex);
        String highlightedText = entityHighlighter.getHighlightedText(text, termMentions, getUser());
        OntologyProperty highlightedTextHdfsPath = ontologyRepository.getProperty(PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH.toString(), getUser());

        Artifact artifact = new Artifact(rowKey);
        artifact.getMetadata().setHighlightedText(highlightedText);
        artifactRepository.save(artifact, getUser().getModelUserContext());

        vertex.removeProperty(PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH.toString());
    }

    @Inject
    public void setEntityHighlighter(EntityHighlighter entityHighlighter) {
        this.entityHighlighter = entityHighlighter;
    }

    @Inject
    @Override
    public void setTermMentionRepository(TermMentionRepository termMentionRepository) {
        this.termMentionRepository = termMentionRepository;
    }
}
