package io.lumify.palantir.mr.mappers;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.palantir.model.PtNoteAndNoteValue;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.Metadata;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;

import java.io.IOException;
import java.util.Date;

public class PtNoteAndNoteValueMapper extends PalantirMapperBase<LongWritable, PtNoteAndNoteValue> {
    private Visibility visibility;
    private VisibilityJson visibilityJson;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        visibility = new LumifyVisibility("").getVisibility();
        visibilityJson = new VisibilityJson();
    }

    @Override
    protected void safeMap(LongWritable key, PtNoteAndNoteValue ptNoteAndNoteValue, Context context) throws Exception {
        context.setStatus(key.toString());

        if (ptNoteAndNoteValue.isDeleted()) {
            return;
        }

        String objectVertexId = PtObjectMapper.getObjectVertexId(ptNoteAndNoteValue.getLinkObjectId());
        VertexBuilder v = prepareVertex(objectVertexId, visibility);
        String propertyKey = getPropertyKey(ptNoteAndNoteValue);
        String propertyValue = getPropertyValue(ptNoteAndNoteValue);
        Metadata propertyMetadata = new Metadata();
        LumifyProperties.CREATED_BY.setMetadata(propertyMetadata, PtUserMapper.getUserVertexId(ptNoteAndNoteValue.getCreatedBy()), visibility);
        LumifyProperties.CREATE_DATE.setMetadata(propertyMetadata, new Date(ptNoteAndNoteValue.getTimeCreated()), visibility);
        LumifyProperties.MODIFIED_BY.setMetadata(propertyMetadata, PtUserMapper.getUserVertexId(ptNoteAndNoteValue.getLastModifiedBy()), visibility);
        LumifyProperties.MODIFIED_DATE.setMetadata(propertyMetadata, new Date(ptNoteAndNoteValue.getLastModified()), visibility);
        LumifyProperties.VISIBILITY_JSON.setMetadata(propertyMetadata, visibilityJson, visibility);
        LumifyProperties.COMMENT.addPropertyValue(v, propertyKey, propertyValue, propertyMetadata, visibility);
        v.save(getAuthorizations());
    }

    private String getPropertyValue(PtNoteAndNoteValue ptNoteAndNoteValue) {
        StringBuilder result = new StringBuilder();
        if (ptNoteAndNoteValue.getTitle() != null) {
            result.append(ptNoteAndNoteValue.getTitle());
            result.append("\n");
        }
        if (ptNoteAndNoteValue.getContents() != null) {
            result.append(ptNoteAndNoteValue.getContents());
        }
        return result.toString().trim();
    }

    private String getPropertyKey(PtNoteAndNoteValue ptNoteAndNoteValue) {
        return Long.toString(ptNoteAndNoteValue.getId());
    }
}
