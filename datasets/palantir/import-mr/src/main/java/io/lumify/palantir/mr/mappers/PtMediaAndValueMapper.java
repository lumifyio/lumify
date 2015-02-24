package io.lumify.palantir.mr.mappers;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.palantir.model.PtMediaAndValue;
import io.lumify.palantir.util.TryInflaterInputStream;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.EdgeBuilderByVertexId;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.securegraph.property.StreamingPropertyValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class PtMediaAndValueMapper extends PalantirMapperBase<LongWritable, PtMediaAndValue> {
    private Visibility visibility;
    private String hasMediaConceptTypeIri;
    private VisibilityJson visibilityJson;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        visibility = new LumifyVisibility("").getVisibility();
        visibilityJson = new VisibilityJson();
        hasMediaConceptTypeIri = getOntologyRepository().getRequiredRelationshipIRIByIntent("hasMedia");
    }

    @Override
    protected void safeMap(LongWritable key, PtMediaAndValue ptMediaAndValue, Context context) throws Exception {
        context.setStatus(key.toString());

        if (ptMediaAndValue.isDeleted()) {
            return;
        }
        if (ptMediaAndValue.getContents() == null) {
            return;
        }

        String propertyKey = getPropertyKey(ptMediaAndValue);
        try (InputStream in = new TryInflaterInputStream(ptMediaAndValue.getContents())) {
            StreamingPropertyValue propertyValue = new StreamingPropertyValue(in, byte[].class);
            propertyValue.store(true);
            propertyValue.searchIndex(false);

            String title = ptMediaAndValue.getTitle();
            if (title == null) {
                title = "";
            }

            VertexBuilder vertexBuilder = prepareVertex(getMediaId(ptMediaAndValue), visibility);
            LumifyProperties.RAW.addPropertyValue(vertexBuilder, propertyKey, propertyValue, visibility);
            LumifyProperties.TITLE.setProperty(vertexBuilder, title, visibility);
            LumifyProperties.CREATED_BY.setProperty(vertexBuilder, PtUserMapper.getUserVertexId(ptMediaAndValue.getCreatedBy()), visibility);
            LumifyProperties.CREATE_DATE.setProperty(vertexBuilder, new Date(ptMediaAndValue.getTimeCreated()), visibility);
            LumifyProperties.MODIFIED_BY.setProperty(vertexBuilder, PtUserMapper.getUserVertexId(ptMediaAndValue.getLastModifiedBy()), visibility);
            LumifyProperties.MODIFIED_DATE.setProperty(vertexBuilder, new Date(ptMediaAndValue.getLastModified()), visibility);
            LumifyProperties.VISIBILITY_JSON.setProperty(vertexBuilder, visibilityJson, visibility);
            Vertex mediaVertex = vertexBuilder.save(getAuthorizations());

            String sourceVertexId = PtObjectMapper.getObjectVertexId(ptMediaAndValue.getLinkObjectId());

            String edgeId = getEdgeId(ptMediaAndValue);
            String edgeLabel = getEdgeLabel(ptMediaAndValue);
            EdgeBuilderByVertexId edgeBuilder = prepareEdge(edgeId, sourceVertexId, mediaVertex.getId(), edgeLabel, visibility);
            LumifyProperties.CREATED_BY.setProperty(edgeBuilder, PtUserMapper.getUserVertexId(ptMediaAndValue.getCreatedBy()), visibility);
            LumifyProperties.CREATE_DATE.setProperty(edgeBuilder, new Date(ptMediaAndValue.getTimeCreated()), visibility);
            LumifyProperties.MODIFIED_BY.setProperty(edgeBuilder, PtUserMapper.getUserVertexId(ptMediaAndValue.getLastModifiedBy()), visibility);
            LumifyProperties.MODIFIED_DATE.setProperty(edgeBuilder, new Date(ptMediaAndValue.getLastModified()), visibility);
            LumifyProperties.VISIBILITY_JSON.setProperty(edgeBuilder, visibilityJson, visibility);
            edgeBuilder.save(getAuthorizations());
        }
    }

    private String getMediaId(PtMediaAndValue ptMediaAndValue) {
        return ID_PREFIX + "_media_" + ptMediaAndValue.getId();
    }

    private String getPropertyKey(PtMediaAndValue ptMediaAndValue) {
        return getBaseIri() + ptMediaAndValue.getId();
    }

    private String getEdgeId(PtMediaAndValue ptMediaAndValue) {
        return ID_PREFIX + "_media_" + ptMediaAndValue.getLinkObjectId() + "_to_" + ptMediaAndValue.getId();
    }

    private String getEdgeLabel(PtMediaAndValue row) {
        return hasMediaConceptTypeIri;
    }
}
