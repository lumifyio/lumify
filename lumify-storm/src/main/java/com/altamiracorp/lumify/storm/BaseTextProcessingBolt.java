package com.altamiracorp.lumify.storm;

import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.TEXT;
import static com.google.common.base.Preconditions.checkNotNull;

import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

public abstract class BaseTextProcessingBolt extends BaseLumifyBolt {
    protected InputStream getInputStream(Vertex graphVertex) throws Exception {
        checkNotNull(graphVertex, "graphVertex cannot be null");

        StreamingPropertyValue textPropertyValue = TEXT.getPropertyValue(graphVertex);
        if (textPropertyValue == null) {
            return new ByteArrayInputStream("".getBytes());
        }
        return textPropertyValue.getInputStream();
    }

    protected String getText(Vertex graphVertex) throws Exception {
        return IOUtils.toString(getInputStream(graphVertex));
    }
}
