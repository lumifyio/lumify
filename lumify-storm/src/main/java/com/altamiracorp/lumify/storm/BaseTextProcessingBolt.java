package com.altamiracorp.lumify.storm;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseTextProcessingBolt extends BaseLumifyBolt {
    protected InputStream getInputStream(Vertex graphVertex) throws Exception {
        checkNotNull(graphVertex, "graphVertex cannot be null");

        StreamingPropertyValue textPropertyValue = (StreamingPropertyValue) graphVertex.getPropertyValue(PropertyName.TEXT.toString(), 0);
        if (textPropertyValue == null) {
            return new ByteArrayInputStream("".getBytes());
        }
        return textPropertyValue.getInputStream();
    }

    protected String getText(Vertex graphVertex) throws Exception {
        return IOUtils.toString(getInputStream(graphVertex));
    }
}
