package io.lumify.palantir.dataImport.model;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.dataImport.formatFunctions.*;
import io.lumify.palantir.dataImport.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisplayFormula {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DisplayFormula.class);
    private static Pattern VALUE_BODY_PATTERN = Pattern.compile("^<VALUE>(.*)</VALUE>$", Pattern.DOTALL);
    private static Pattern UNPARSED_VALUE_BODY_PATTERN = Pattern.compile("^<UNPARSED_VALUE>(.*)</UNPARSED_VALUE>$", Pattern.DOTALL);
    private static Pattern VALUE_SUBSTITUTION = Pattern.compile("\\{(.*?)\\}");
    private static final DocumentBuilder dBuilder;
    private static Map<String, FormatFunctionBase> formatFunctions = new HashMap<String, FormatFunctionBase>();
    private boolean prettyPrint;
    private List<String> formulas = new ArrayList<String>();

    static {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LumifyException("Could not create document builder", e);
        }

        formatFunctions.put("add_ssn_dashes", new AddSsnDashesFormatFunction());
        formatFunctions.put("uppercase", new UppercaseFormatFunction());
        formatFunctions.put("lowercase", new LowercaseFormatFunction());
        formatFunctions.put("add_number_commas", new AddNumberCommasFormatFunction());
        formatFunctions.put("add_phone_dashes", new AddPhoneDashesFormatFunction());
        formatFunctions.put("smart_spacer", new SmartSpacerFormatFunction());
        formatFunctions.put("money", new MoneyFormatFunction());
    }

    public DisplayFormula(Element displayElement) {
        if (displayElement == null) {
            return;
        }

        Element argsElement = XmlUtil.getChildByTagName(displayElement, "args");
        if (argsElement == null) {
            return;
        }

        NodeList argElements = argsElement.getChildNodes();
        for (int i = 0; i < argElements.getLength(); i++) {
            Node argElement = argElements.item(i);
            if (!(argElement instanceof Element)) {
                continue;
            }
            if (!((Element) argElement).getTagName().equals("arg")) {
                continue;
            }
            String arg = argElement.getTextContent();
            if (arg.startsWith("prettyprint=")) {
                prettyPrint = Boolean.parseBoolean(arg.substring("prettyprint=".length()));
                continue;
            }
            if (arg.startsWith("tokens=")) {
                formulas.add(arg.substring("tokens=".length()));
                continue;
            }
            throw new LumifyException("Could not parse arg formula " + arg);
        }
    }

    public Object toValue(String value) {
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

        if (formulas.size() > 0) {
            Map<String, String> values = getValuesFromValue(value);
            String formattedValue = formatValues(values);
            if (formattedValue != null) {
                return formattedValue;
            }
        }

        return value;
    }

    private String formatValues(Map<String, String> values) {
        for (String displayFormula : formulas) {
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
                if (exprParts.length > 1) {
                    String fn = exprParts[1].trim();
                    v = applyFormatFunction(fn, v);
                }
                if (prettyPrint) {
                    if (v.length() > 0) {
                        v = Character.toUpperCase(v.charAt(0)) + v.substring(1);
                    }
                }
                matcher.appendReplacement(output, Matcher.quoteReplacement(v));
            }
            matcher.appendTail(output);
            return output.toString();
        } catch (Exception ex) {
            throw new LumifyException("Could not format using formula: " + displayFormula, ex);
        }
    }

    private String applyFormatFunction(String fn, String value) {
        fn = fn.trim().toLowerCase();
        FormatFunctionBase formatFn = formatFunctions.get(fn);
        if (formatFn != null) {
            return formatFn.format(value);
        }
        LOGGER.error("Unknown format function: %s", fn);
        return value;
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
