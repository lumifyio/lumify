package io.lumify.palantir.ontologyToOwl;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleFormulaMaker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TitleFormulaMaker.class);
    private static Pattern PATTERN_PROPERTY = Pattern.compile("\\{(.*?),(.*?)\\}");
    private static Pattern PATTERN_LONGEST_PROPERTY = Pattern.compile("\\{LONGEST_PROPERTY\\}");
    private final String baseUri;

    public TitleFormulaMaker(String baseUri) {
        this.baseUri = baseUri;
    }

    public String create(List<Element> titleArgs) {
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (Element titleArg : titleArgs) {
            if (count > 0) {
                result.append('\n');
            }
            String titleArgStr = titleArg.getTextContent();
            try {
                result.append(createFromArg(titleArgStr));
            } catch (Exception ex) {
                LOGGER.error("Could not process title arg: " + titleArgStr, ex);
            }
            count++;
        }
        return result.toString();
    }

    private String createFromArg(String arg) {
        arg = arg.replaceAll("\\{LABEL_PROPERTY\\}", "{NONE,label}");

        StringBuilder result = new StringBuilder();
        List<String> conditionals = getConditionals(arg);
        if (conditionals.size() > 0) {
            result.append("if (");
            int count = 0;
            for (String conditional : conditionals) {
                if (count > 0) {
                    result.append(" && ");
                }
                result.append(conditional);
                count++;
            }
            result.append(") {\n  ");
        }

        result.append(getReturnStatement(arg));

        if (conditionals.size() > 0) {
            result.append("}\n");
        }

        return result.toString();
    }

    private String getReturnStatement(String arg) {
        StringBuilder result = new StringBuilder();
        result.append("return ");

        String workingString = "'" + arg + "'";

        StringBuffer temp = new StringBuffer();
        Matcher m = PATTERN_PROPERTY.matcher(workingString);
        while (m.find()) {
            if (!"NONE".equals(m.group(1))) {
                throw new LumifyException("Unhandled title formula property: " + arg);
            }
            String iri = uriToIri(m.group(2));
            m.appendReplacement(temp, "' + prop('" + iri + "') + '");
        }
        m.appendTail(temp);
        workingString = temp.toString();

        temp = new StringBuffer();
        m = PATTERN_LONGEST_PROPERTY.matcher(workingString);
        while (m.find()) {
            m.appendReplacement(temp, "' + longestProp() + '");
        }
        m.appendTail(temp);
        workingString = temp.toString();

        if (workingString.startsWith("'' + ")) {
            workingString = workingString.substring("'' + ".length());
        }
        if (workingString.endsWith(" + ''")) {
            workingString = workingString.substring(0, workingString.length() - " + ''".length());
        }
        result.append(workingString);

        result.append(";\n");
        return result.toString();
    }

    private List<String> getConditionals(String arg) {
        List<String> results = new ArrayList<String>();

        Matcher m = PATTERN_PROPERTY.matcher(arg);
        while (m.find()) {
            String uri = m.group(2);
            results.add("prop('" + uriToIri(uri) + "')");
        }

        m = PATTERN_LONGEST_PROPERTY.matcher(arg);
        while (m.find()) {
            results.add("longestProp()");
        }

        return results;
    }

    private String uriToIri(String uri) {
        return OntologyToOwl.uriToIri(baseUri, uri);
    }
}
