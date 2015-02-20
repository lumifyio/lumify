package io.lumify.palantir.mr.mappers;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.palantir.model.PtPropertyAndValue;
import io.lumify.palantir.model.PtPropertyType;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PtPropertyAndValueMapper extends PalantirMapperBase<LongWritable, PtPropertyAndValue> {
    private Visibility visibility;
    private DocumentBuilder dBuilder;
    private static Pattern VALUE_BODY_PATTERN = Pattern.compile("^<VALUE>(.*)</VALUE>$", Pattern.DOTALL);
    private static Pattern UNPARSED_VALUE_BODY_PATTERN = Pattern.compile("^<UNPARSED_VALUE>(.*)</UNPARSED_VALUE>$", Pattern.DOTALL);

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        loadPropertyTypes(context);
        visibility = new LumifyVisibility("").getVisibility();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LumifyException("Could not create document builder", e);
        }
    }

    @Override
    protected void safeMap(LongWritable key, PtPropertyAndValue ptPropertyAndValue, Context context) throws Exception {
        context.setStatus(key.toString());

        PtPropertyType propertyType = getPropertyType(ptPropertyAndValue.getType());
        if (propertyType == null) {
            throw new LumifyException("Could not find property type: " + ptPropertyAndValue.getType());
        }

        String objectVertexId = PtObjectMapper.getObjectVertexId(ptPropertyAndValue.getLinkObjectId());
        String propertyKey = getPropertyKey(ptPropertyAndValue);
        Value value = cleanUpValueString(ptPropertyAndValue.getValue());
        if (value == null) {
            return;
        }

        VertexBuilder v = prepareVertex(objectVertexId, visibility);
        if (value instanceof StringValue) {
            String propertyName = getPropertyName(propertyType.getUri(), null);
            Object valueObject = toValue(propertyType, null, ((StringValue) value).getValue());
            v.addPropertyValue(propertyKey, propertyName, valueObject, visibility);
        } else if (value instanceof MapValue) {
            MapValue values = (MapValue) value;
            for (Map.Entry<String, String> valueEntry : values.getValues().entrySet()) {
                String propertyName = getPropertyName(propertyType.getUri(), valueEntry.getKey());
                Object valueObject = toValue(propertyType, valueEntry.getKey(), valueEntry.getValue());
                v.addPropertyValue(propertyKey, propertyName, valueObject, visibility);
            }
        } else {
            throw new RuntimeException("Unexpected value type: " + value.getClass().getName());
        }
        v.save(getAuthorizations());
    }

    private Object toValue(PtPropertyType propertyType, String innerKey, String value) {
        return value;
    }

    private String getPropertyName(String uri, String innerKey) {
        return getBaseIri() + uri + (innerKey == null ? "" : ("/" + innerKey));
    }

    private Value cleanUpValueString(String value) throws IOException, SAXException {
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.equals("<null></null>")) {
            return null;
        }

        Matcher m = VALUE_BODY_PATTERN.matcher(value);
        if (m.matches()) {
            value = m.group(1).trim();
        }

        m = VALUE_BODY_PATTERN.matcher(value);
        if (m.matches()) {
            return new StringValue(m.group(1).trim());
        }

        m = UNPARSED_VALUE_BODY_PATTERN.matcher(value);
        if (m.matches()) {
            return new StringValue(m.group(1).trim());
        }

        Document d = dBuilder.parse(new ByteArrayInputStream(("<v>" + value + "</v>").getBytes()));
        Map<String, String> values = new HashMap<>();
        NodeList childNodes = d.getDocumentElement().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode instanceof Element) {
                Element e = (Element) childNode;
                String tagName = e.getTagName();
                values.put(tagName, e.getTextContent());
            }
        }
        return new MapValue(values);
    }

    private String getPropertyKey(PtPropertyAndValue ptPropertyAndValue) {
        return ID_PREFIX + ptPropertyAndValue.getPropertyValueId();
    }

    private static abstract class Value {

    }

    private static class StringValue extends Value {
        private final String value;

        public StringValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static class MapValue extends Value {
        private final Map<String, String> values;

        public MapValue(Map<String, String> values) {
            this.values = values;
        }

        public Map<String, String> getValues() {
            return values;
        }
    }
}
