package io.lumify.core.ingest;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.GraphUtil;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.Element;
import org.securegraph.Metadata;
import org.securegraph.Property;
import org.securegraph.Visibility;
import org.securegraph.mutation.ElementMutation;
import org.securegraph.property.StreamingPropertyValue;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class MetadataGraphPropertyWorker extends GraphPropertyWorker {

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        JSONObject metadataJson = getMetadataJson(data);

        JSONArray propertiesJson = metadataJson.optJSONArray("properties");
        if (propertiesJson == null) {
            return;
        }

        for (int i = 0; i < propertiesJson.length(); i++) {
            JSONObject propertyJson = propertiesJson.getJSONObject(i);
            setProperty(propertyJson, data);
        }

        getGraph().flush();

        for (int i = 0; i < propertiesJson.length(); i++) {
            JSONObject propertyJson = propertiesJson.getJSONObject(i);
            queueProperty(propertyJson, data);
        }
    }

    public void queueProperty(JSONObject propertyJson, GraphPropertyWorkData data) {
        String propertyKey = propertyJson.optString("key");
        if (propertyKey == null) {
            propertyKey = ElementMutation.DEFAULT_KEY;
        }
        String propertyName = propertyJson.optString("name");
        getWorkQueueRepository().pushGraphPropertyQueue(data.getElement(), propertyKey, propertyName);
    }

    private void setProperty(JSONObject propertyJson, GraphPropertyWorkData data) {
        String propertyKey = propertyJson.optString("key", null);
        if (propertyKey == null) {
            propertyKey = ElementMutation.DEFAULT_KEY;
        }

        String propertyName = propertyJson.optString("name", null);
        checkNotNull(propertyName, "name is required: " + propertyJson.toString());

        String propertyValue = propertyJson.optString("value", null);
        checkNotNull(propertyValue, "value is required: " + propertyJson.toString());

        String visibilitySource = propertyJson.optString("visibilitySource", null);
        Visibility visibility;
        if (visibilitySource == null) {
            visibility = data.getVisibility();
        } else {
            visibility = new Visibility(visibilitySource);
        }

        Metadata metadata = new Metadata();
        LumifyProperties.VISIBILITY_JSON.setMetadata(metadata, GraphUtil.updateVisibilitySource(null, visibilitySource), getVisibilityTranslator().getDefaultVisibility());

        data.getElement().addPropertyValue(propertyKey, propertyName, propertyValue, metadata, visibility, getAuthorizations());
    }

    public JSONObject getMetadataJson(GraphPropertyWorkData data) throws IOException {
        StreamingPropertyValue metadataJsonValue = LumifyProperties.METADATA_JSON.getPropertyValue(data.getElement());
        InputStream metadataJsonIn = metadataJsonValue.getInputStream();
        try {
            String metadataJsonString = IOUtils.toString(metadataJsonIn);
            return new JSONObject(metadataJsonString);
        } finally {
            metadataJsonIn.close();
        }
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property != null) {
            return false;
        }

        StreamingPropertyValue mappingJson = LumifyProperties.METADATA_JSON.getPropertyValue(element);
        if (mappingJson == null) {
            return false;
        }

        return true;
    }
}
