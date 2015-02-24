package io.lumify.palantir.mr.mappers;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.palantir.model.PtObject;
import io.lumify.palantir.model.PtObjectType;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;

import java.io.IOException;

public class PtObjectMapper extends PalantirMapperBase<LongWritable, PtObject> {
    private Visibility visibility;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        loadObjectTypes(context);
        visibility = new LumifyVisibility("").getVisibility();
    }

    @Override
    protected void safeMap(LongWritable key, PtObject ptObject, Context context) throws Exception {
        context.setStatus(key.toString());

        if (ptObject.getDeleted() != 0) {
            return;
        }

        String conceptTypeUri = getConceptTypeUri(ptObject);

        VertexBuilder m = prepareVertex(getObjectVertexId(ptObject), visibility);
        LumifyProperties.CONCEPT_TYPE.setProperty(m, conceptTypeUri, visibility);
        m.save(getAuthorizations());
    }

    private String getConceptTypeUri(PtObject ptObject) {
        PtObjectType ptObjectType = getObjectType(ptObject.getType());
        if (ptObjectType == null) {
            throw new LumifyException("Could not find object type: " + ptObject.getType());
        }
        return getConceptTypeUri(ptObjectType.getUri());
    }

    protected String getConceptTypeUri(String uri) {
        return getBaseIri() + uri;
    }

    public static String getObjectVertexId(PtObject ptObject) {
        return getObjectVertexId(ptObject.getObjectId());
    }

    public static String getObjectVertexId(long objectId) {
        return ID_PREFIX + "OBJECT_" + objectId;
    }
}
