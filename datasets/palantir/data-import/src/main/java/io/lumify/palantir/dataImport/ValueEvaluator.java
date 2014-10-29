package io.lumify.palantir.dataImport;

import io.lumify.core.exception.LumifyException;
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

public class ValueEvaluator {
    private static Pattern VALUE_BODY_PATTERN = Pattern.compile("^<VALUE>(.*)</VALUE>$", Pattern.DOTALL);
    private static Pattern UNPARSED_VALUE_BODY_PATTERN = Pattern.compile("^<UNPARSED_VALUE>(.*)</UNPARSED_VALUE>$", Pattern.DOTALL);
    private static Pattern VALUE_SUBSTITUTION = Pattern.compile("\\{(.*?)\\}");
    private static final DocumentBuilder dBuilder;

    static {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LumifyException("Could not create document builder", e);
        }
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
                matcher.appendReplacement(output, Matcher.quoteReplacement(v));
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
