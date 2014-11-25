package io.lumify.assignimagemr;

import io.lumify.core.mapreduce.LumifyElementMapperBase;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.securegraph.Direction;
import org.securegraph.Element;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;

import java.io.IOException;

import static com.google.common.collect.Iterables.isEmpty;

public class AssignImageMRMapper extends LumifyElementMapperBase<Text, Element> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AssignImageMRMapper.class);
    private static final String PROPERTY_KEY = AssignImageMRMapper.class.getName();
    private Counter elementsProcessedCounter;
    private Counter assignmentsMadeCounter;
    private AssignImageConfiguration config;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        this.elementsProcessedCounter = context.getCounter(AssignImageCounters.ELEMENTS_PROCESSED);
        this.assignmentsMadeCounter = context.getCounter(AssignImageCounters.ASSIGNMENTS_MADE);
        this.config = new AssignImageConfiguration(context.getConfiguration());
    }

    @Override
    protected void safeMap(Text key, Element element, Context context) throws Exception {
        Vertex vertex = (Vertex) element;
        context.setStatus("Processing " + vertex.getId());

        if (isEmpty(vertex.getProperties(LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyName()))
                && isEmpty(vertex.getProperties(LumifyProperties.ENTITY_IMAGE_URL.getPropertyName()))) {
            String imageVertexId = findBestImageVertexId(vertex);
            if (imageVertexId != null) {
                VertexBuilder m = prepareVertex(vertex.getId(), vertex.getVisibility());
                LumifyProperties.ENTITY_IMAGE_VERTEX_ID.addPropertyValue(m, PROPERTY_KEY, imageVertexId, this.config.getVisibility());
                m.save(this.config.getAuthorizations());
                assignmentsMadeCounter.increment(1);
            }
        }

        elementsProcessedCounter.increment(1);
    }

    private String findBestImageVertexId(Vertex vertex) {
        Iterable<Vertex> vertices = vertex.getVertices(Direction.OUT, config.getHasImageLabels(), config.getAuthorizations());
        for (Vertex v : vertices) {
            String mimeType = LumifyProperties.MIME_TYPE.getPropertyValue(v);
            if (mimeType != null && mimeType.startsWith("image")) {
                return v.getId();
            }
        }
        return null;
    }
}
