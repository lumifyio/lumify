package com.altamiracorp.lumify.storm.textHighlighting;

import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.storm.BaseTextProcessingBolt;
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

        GraphVertex graphVertex = graph.findVertex(graphVertexId, getUser());
        if (graphVertex != null) {
            String artifactRowKey = (String) graphVertex.getProperty(PropertyName.ROW_KEY);
            LOGGER.debug("Processing graph vertex [%s] for artifact: %s", graphVertex.getId(), artifactRowKey);

            Iterable<TermMentionModel> termMentions = termMentionRepository.findByGraphVertexId(graphVertex.getId(), getUser());
            performHighlighting(artifactRowKey, graphVertex, termMentions);
        } else {
            LOGGER.warn("Could not find vertex with id: %s", graphVertexId);
        }
    }

    private void performHighlighting(final String rowKey, final GraphVertex vertex, final Iterable<TermMentionModel> termMentions) throws Exception {
        String text = getText(vertex);
        String highlightedText = entityHighlighter.getHighlightedText(text, termMentions, getUser());

        Artifact artifact = new Artifact(rowKey);
        artifact.getMetadata().setHighlightedText(highlightedText);
        artifactRepository.save(artifact, getUser().getModelUserContext());

        vertex.removeProperty(PropertyName.HIGHLIGHTED_TEXT_HDFS_PATH.toString());
        graph.save(vertex, getUser());
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
