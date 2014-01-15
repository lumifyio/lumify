package com.altamiracorp.lumify.storm;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.securegraph.Vertex;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseTextProcessingBolt extends BaseLumifyBolt {
    protected InputStream getInputStream(Vertex graphVertex) throws Exception {
        checkNotNull(graphVertex, "graphVertex cannot be null");

        InputStream textIn;
        String textHdfsPath = (String) graphVertex.getPropertyValue(PropertyName.TEXT_HDFS_PATH.toString(), 0);
        if (textHdfsPath != null) {
            textIn = openFile(textHdfsPath);
        } else {
            String artifactRowKey = (String) graphVertex.getPropertyValue(PropertyName.ROW_KEY.toString(), 0);
            Artifact artifact = artifactRepository.findByRowKey(artifactRowKey, getUser().getModelUserContext());
            String text = artifact.getMetadata().getText();
            if (text == null) {
                text = "";
            }
            textIn = new ByteArrayInputStream(text.getBytes());
        }
        return textIn;
    }

    protected String getText(Vertex graphVertex) throws Exception {
        return IOUtils.toString(getInputStream(graphVertex));
    }
}
