package io.lumify.tesseract;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.test.GraphPropertyWorkerTestBase;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.securegraph.Property;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import org.securegraph.property.StreamingPropertyValue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.securegraph.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class TesseractGraphPropertyWorkerTest extends GraphPropertyWorkerTestBase {
    private Visibility visibility = new Visibility("");

    @Test
    public void testTesseractTestImage01() throws Exception {
        byte[] imageData = getResourceAsByteArray(TesseractGraphPropertyWorkerTest.class, "testImage01.jpg");

        Map<String, Object> metadata = new HashMap<String, Object>();
        LumifyProperties.MIME_TYPE.setMetadata(metadata, "image/jpg");
        StreamingPropertyValue value = new StreamingPropertyValue(new ByteArrayInputStream(imageData), byte[].class);
        Vertex v1 = getGraph().prepareVertex("v1", visibility)
                .addPropertyValue("k1", "image", value, metadata, visibility)
                .save(getGraphAuthorizations());

        TesseractGraphPropertyWorker gpw = new TesseractGraphPropertyWorker();
        run(gpw, getWorkerPrepareData(), v1, v1.getProperty("k1", "image"), new ByteArrayInputStream(imageData));

        v1 = getGraph().getVertex("v1", getGraphAuthorizations());
        List<Property> textProperties = toList(LumifyProperties.TEXT.getProperties(v1));
        assertEquals(1, textProperties.size());
        Property textProperty = textProperties.get(0);
        StreamingPropertyValue textValue = (StreamingPropertyValue) textProperty.getValue();
        assertNotNull("textValue was null", textValue);
        String textValueString = IOUtils.toString(textValue.getInputStream());
        assertTrue("does not contain Tesseract", textValueString.contains("Tesseract"));
        assertTrue("does not contain According to the Oxford English Dictionary", textValueString.contains("According to the Oxford English Dictionary"));

        assertEquals(1, getGraphPropertyQueue().size());
        JSONObject graphPropertyQueueItem = getGraphPropertyQueue().peek();
        assertEquals(textProperty.getName(), graphPropertyQueueItem.getString("propertyName"));
        assertEquals(textProperty.getKey(), graphPropertyQueueItem.getString("propertyKey"));
        assertEquals(v1.getId(), graphPropertyQueueItem.getString("graphVertexId"));
    }

    @Override
    protected Map getConfigurationMap() {
        Map map = super.getConfigurationMap();
        File tessdataDir = new File("/usr/share/tesseract-ocr/tessdata/");
        if (!tessdataDir.exists()) {
            throw new LumifyException("Could not find tessdata path: " + tessdataDir.getAbsolutePath());
        }
        map.put("tesseract.dataPath", tessdataDir.getAbsolutePath());
        return map;
    }
}