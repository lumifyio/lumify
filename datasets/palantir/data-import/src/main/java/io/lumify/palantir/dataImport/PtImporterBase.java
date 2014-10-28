package io.lumify.palantir.dataImport;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.dataImport.model.PtObject;
import io.lumify.palantir.dataImport.model.PtObjectObject;
import io.lumify.palantir.dataImport.model.PtPropertyAndValue;
import io.lumify.palantir.dataImport.model.PtPropertyType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PtImporterBase<T> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PtImporterBase.class);
    private static Pattern VALUE_BODY_PATTERN = Pattern.compile("^<VALUE>(.*)</VALUE>$", Pattern.DOTALL);
    private static Pattern UNPARSED_VALUE_BODY_PATTERN = Pattern.compile("^<UNPARSED_VALUE>(.*)</UNPARSED_VALUE>$", Pattern.DOTALL);
    private static Pattern VALUE_SUBSTITUTION = Pattern.compile("\\{(.*?)\\}");
    private final Class<T> ptClass;
    private final DataImporter dataImporter;
    private static final DocumentBuilder dBuilder;

    static {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LumifyException("Could not create document builder", e);
        }
    }

    protected PtImporterBase(DataImporter dataImporter, Class<T> ptClass) {
        this.dataImporter = dataImporter;
        this.ptClass = ptClass;
    }

    public void run() {
        int count = 0;
        LOGGER.info(this.getClass().getName());
        Iterable<T> rows = dataImporter.getSqlRunner().select(getSql(), ptClass);
        beforeProcessRows();
        for (T row : rows) {
            if (count % 1000 == 0) {
                LOGGER.debug("Importing %s: %d", this.ptClass.getSimpleName(), count);
                dataImporter.getGraph().flush();
            }

            try {
                processRow(row);
            } catch (Throwable ex) {
                handleProcessRowError(row, ex);
            }
            count++;
        }
        afterProcessRows();
        LOGGER.info("Imported %d %s", count, this.ptClass.getSimpleName());
    }

    private void handleProcessRowError(T row, Throwable ex) {
        LOGGER.error("Could not process row: %s (type: %s)", row, ptClass.getSimpleName(), ex);
    }

    protected void afterProcessRows() {

    }

    protected void beforeProcessRows() {

    }

    public DataImporter getDataImporter() {
        return dataImporter;
    }

    protected abstract void processRow(T row) throws Exception;

    protected abstract String getSql();

    protected String getConceptTypeUri(String uri) {
        return getDataImporter().getOwlPrefix() + uri;
    }

    protected String getPropertyName(String uri) {
        return getDataImporter().getOwlPrefix() + uri;
    }

    protected String getLinkTypeUri(String uri) {
        return getDataImporter().getOwlPrefix() + uri;
    }

    protected String getEdgeId(PtObjectObject ptObjectObject) {
        return getDataImporter().getIdPrefix() + ptObjectObject.getLinkId();
    }

    protected String getObjectId(long objectId) {
        return getDataImporter().getIdPrefix() + objectId;
    }

    protected String getObjectId(PtPropertyAndValue ptPropertyAndValue) {
        return getDataImporter().getIdPrefix() + ptPropertyAndValue.getLinkObjectId();
    }

    protected String getObjectId(PtObject ptObject) {
        return getDataImporter().getIdPrefix() + ptObject.getObjectId();
    }

    protected Object toValue(String value, PtPropertyType propertyType) {
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

        m = UNPARSED_VALUE_BODY_PATTERN.matcher(value);
        if (m.matches()) {
            return m.group(1).trim();
        }

        if (propertyType.getDisplayFormulas().size() > 0) {
            Map<String, String> values = getValuesFromValue(value);
            String formattedValue = formatValues(values, propertyType);
            if (formattedValue != null) {
                return formattedValue;
            }
        }

        return value;
    }

    private String formatValues(Map<String, String> values, PtPropertyType propertyType) {
        for (String displayFormula : propertyType.getDisplayFormulas()) {
            String r = formatValues(values, displayFormula);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    private String formatValues(Map<String, String> values, String displayFormula) {
        try {
            StringBuffer output = new StringBuffer();
            Matcher matcher = VALUE_SUBSTITUTION.matcher(displayFormula);
            while (matcher.find()) {
                String expr = matcher.group(1);
                String[] exprParts = expr.split(",");
                String v = values.get(exprParts[0]);
                if (v == null) {
                    return null; // could not find a value to match replacement
                }
                matcher.appendReplacement(output, v);
            }
            matcher.appendTail(output);
            return output.toString();
        } catch (Exception ex) {
            throw new LumifyException("Could not format using formula: " + displayFormula, ex);
        }
    }

    private Map<String, String> getValuesFromValue(String value) {
        try {
            Document d = dBuilder.parse(new ByteArrayInputStream(("<v>" + value + "</v>").getBytes()));
            Map<String, String> values = new HashMap<String, String>();
            NodeList childNodes = d.getDocumentElement().getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode instanceof Element) {
                    Element e = (Element) childNode;
                    values.put(e.getTagName(), e.getTextContent());
                }
            }
            return values;
        } catch (Exception e) {
            throw new LumifyException("Could not parse value into values: " + value, e);
        }
    }
}
