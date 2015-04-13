package io.lumify.palantir.mr.mappers;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.palantir.model.PtLinkType;
import io.lumify.palantir.model.PtObjectObject;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.EdgeBuilderByVertexId;
import org.securegraph.Visibility;

import java.io.IOException;
import java.util.Date;

public class PtObjectObjectMapper extends PalantirMapperBase<LongWritable, PtObjectObject> {
    private Visibility visibility;
    private VisibilityJson visibilityJson;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        loadLinkTypes(context);
        visibility = new LumifyVisibility("").getVisibility();
        visibilityJson = new VisibilityJson();
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
        LumifyProperties.CREATED_BY.setProperty(m, PtUserMapper.getUserVertexId(ptObjectObject.getCreatedBy()), visibility);
        LumifyProperties.CREATE_DATE.setProperty(m, new Date(ptObjectObject.getTimeCreated()), visibility);
        LumifyProperties.MODIFIED_BY.setProperty(m, PtUserMapper.getUserVertexId(ptObjectObject.getLastModifiedBy()), visibility);
        LumifyProperties.MODIFIED_DATE.setProperty(m, new Date(ptObjectObject.getLastModified()), visibility);
        LumifyProperties.VISIBILITY_JSON.setProperty(m, visibilityJson, visibility);
        m.save(getAuthorizations());
    }

    protected String getLinkTypeUri(String uri) {
        return getBaseIri() + uri;
    }
}
