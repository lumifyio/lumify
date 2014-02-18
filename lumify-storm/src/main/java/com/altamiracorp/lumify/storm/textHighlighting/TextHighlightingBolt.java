package com.altamiracorp.lumify.storm.textHighlighting;

import backtype.storm.tuple.Tuple;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.storm.BaseTextProcessingBolt;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;

import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.HIGHLIGHTED_TEXT;

public class TextHighlightingBolt extends BaseTextProcessingBolt {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TextHighlightingBolt.class);
    private TermMentionRepository termMentionRepository;
    private EntityHighlighter entityHighlighter;

    @Override
    protected void safeExecute(Tuple input) throws Exception {
        final JSONObject json = getJsonFromTuple(input);
        final String graphVertexId = json.getString("graphVertexId");

        Vertex artifactGraphVertex = graph.getVertex(graphVertexId, getAuthorizations());
        if (artifactGraphVertex != null) {
            LOGGER.debug("Processing graph vertex [%s]", artifactGraphVertex.getId());

            Iterable<TermMentionModel> termMentions = termMentionRepository.findByGraphVertexId(artifactGraphVertex.getId().toString(), getUser());
            performHighlighting(artifactGraphVertex, termMentions);
            graph.flush();
        } else {
            LOGGER.warn("Could not find vertex with id: %s", graphVertexId);
        }
    }

    private void performHighlighting(final Vertex vertex, final Iterable<TermMentionModel> termMentions) throws Exception {
        String text = getText(vertex);
        String highlightedText = entityHighlighter.getHighlightedText(text, termMentions, getAuthorizations());

        StreamingPropertyValue highlightedTextPropertyValue = new StreamingPropertyValue(new ByteArrayInputStream(highlightedText.getBytes()), String.class);
        highlightedTextPropertyValue.store(true);
        highlightedTextPropertyValue.searchIndex(true);
        HIGHLIGHTED_TEXT.setProperty(vertex, highlightedTextPropertyValue, new Visibility(""));
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
