package io.lumify.palantir.mr.mappers;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.palantir.model.PtLinkType;
import io.lumify.palantir.model.PtObjectObject;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.EdgeBuilderByVertexId;
import org.securegraph.Visibility;

import java.io.IOException;

public class PtObjectObjectMapper extends PalantirMapperBase<LongWritable, PtObjectObject> {
    private Visibility visibility;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        loadLinkTypes(context);
        visibility = new LumifyVisibility("").getVisibility();
    }

    @Override
    protected void safeMap(LongWritable key, PtObjectObject ptObjectObject, Context context) throws Exception {
        context.setStatus(key.toString());

        if (ptObjectObject.isDeleted()) {
            return;
        }

        String sourceVertexId = PtObjectMapper.getObjectVertexId(ptObjectObject.getParentObjectId());
        String destVertexId = PtObjectMapper.getObjectVertexId(ptObjectObject.getChildObjectId());

        PtLinkType ptLinkType = getLinkType(ptObjectObject.getType());
        if (ptLinkType == null) {
            throw new LumifyException("Could not find link type: " + ptObjectObject.getType());
        }
        String linkTypeUri = getLinkTypeUri(ptLinkType.getUri());

        String edgeId = sourceVertexId + linkTypeUri + destVertexId;
        EdgeBuilderByVertexId m = prepareEdge(edgeId, sourceVertexId, destVertexId, linkTypeUri, visibility);
        m.save(getAuthorizations());
    }

    protected String getLinkTypeUri(String uri) {
        return getBaseIri() + uri;
    }
}
