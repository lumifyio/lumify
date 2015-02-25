package io.lumify.palantir.ontologyToOwl;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleFormulaMaker {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TitleFormulaMaker.class);
    public static final String PALANTIR_PRETTY_PRINT = "palantirPrettyPrint";
    private static Pattern PATTERN_PROPERTY = Pattern.compile("\\{(.*?)\\}");

    public String create(Options options, List<Element> titleArgs) {
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (Element titleArg : titleArgs) {
            if (count > 0) {
                result.append('\n');
            }
            String titleArgStr = titleArg.getTextContent();
            try {
                String fromArg = createFromArg(options, titleArgStr);
                if (fromArg != null) {
                    result.append(fromArg);
                }
            } catch (Exception ex) {
                LOGGER.error("Could not process title arg: " + titleArgStr, ex);
            }
            count++;
        }
        return result.toString();
    }

    private String createFromArg(Options options, String arg) {
        if (arg.startsWith("tokens=")) {
            arg = arg.substring("tokens=".length());
        }
        if (arg.startsWith("prettyprint=")) {
            options.setPrettyPrint(Boolean.parseBoolean(arg.substring("prettyprint=".length()).trim()));
            return null;
        }

        arg = arg.replaceAll("\\{LABEL_PROPERTY\\}", "{NONE,com.palantir.property.IntrinsicTitle}");

        StringBuilder result = new StringBuilder();
        List<String> conditionals = getConditionals(options, arg);
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

        result.append(getReturnStatement(options, arg));

        if (conditionals.size() > 0) {
            result.append("}\n");
        }

        return result.toString();
    }

    private String getReturnStatement(Options options, String arg) {
        StringBuilder result = new StringBuilder();
        result.append("return ");

        String workingString = "'" + arg + "'";

        StringBuffer temp = new StringBuffer();
        Matcher m = PATTERN_PROPERTY.matcher(workingString);
        while (m.find()) {
            PatternFieldInfo pfi = new PatternFieldInfo(options, m.group(1));
            m.appendReplacement(temp, "' + " + pfi.toCall(uriToIri(options, pfi.getFieldName())) + " + '");
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

    private List<String> getConditionals(Options options, String arg) {
        List<String> results = new ArrayList<>();

        Matcher m = PATTERN_PROPERTY.matcher(arg);
        while (m.find()) {
            PatternFieldInfo pfi = new PatternFieldInfo(options, m.group(1));
            String uri = pfi.getFieldName();
            results.add(pfi.toCall(uriToIri(options, uri)));
        }

        return results;
    }

    private String uriToIri(Options options, String uri) {
        return options.getBaseIri() + uri;
    }

    public static class Options {
        private final String baseIri;
        private boolean prettyPrint;

        public Options(String baseIri) {
            this.baseIri = baseIri;
        }

        public String getBaseIri() {
            return baseIri;
        }

        public boolean isPrettyPrint() {
            return prettyPrint;
        }

        public void setPrettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
        }
    }

    private static class PatternFieldInfo {
        private final String fieldName;
        private final String functionName;
        private final JSONObject propOptions;

        public PatternFieldInfo(Options options, String str) {
            String[] matchDataParts = str.split(",");
            String fieldName;
            String fn = "prop";
            propOptions = new JSONObject();

            if (options.isPrettyPrint()) {
                propOptions.put(PALANTIR_PRETTY_PRINT, true);
            }

            if (matchDataParts.length == 2) {
                if ("NONE".equals(matchDataParts[0])) {
                    fieldName = matchDataParts[1];
                } else {
                    fieldName = matchDataParts[0];
                    String fnStr = matchDataParts[1].trim();
                    if (fnStr.equalsIgnoreCase("uppercase")) {
                        propOptions.put("uppercase", true);
                        propOptions.remove(PALANTIR_PRETTY_PRINT);
                    } else if (fnStr.equalsIgnoreCase("lowercase")) {
                        propOptions.put("lowercase", true);
                        propOptions.remove(PALANTIR_PRETTY_PRINT);
                    } else if (fnStr.equalsIgnoreCase("smart_spacer")) {
                        propOptions.put("smartSpacer", true);
                    } else if (fnStr.equalsIgnoreCase("money")) {
                        propOptions.put("money", true);
                    } else if (fnStr.equalsIgnoreCase("add_phone_dashes")) {
                        propOptions.put("addPhoneDashes", true);
                    } else {
                        LOGGER.warn("Unhandled function: %s for format %s", fnStr, str);
                    }
                }
            } else if (matchDataParts[0].equals("LONGEST_PROPERTY")) {
                fieldName = null;
                fn = "longestProp";
            } else {
                fieldName = matchDataParts[0];
            }
            this.fieldName = fieldName;
            this.functionName = fn;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFunctionName() {
            return functionName;
        }

        public JSONObject getPropOptions() {
            return propOptions;
        }

        public String toCall(String fieldUri) {
            String args;
            if (getFieldName() == null) {
                args = "";
            } else {
                args = "'" + fieldUri + "'";
            }
            if (getPropOptions().length() > 0) {
                if (args.length() > 0) {
                    args += ", ";
                }
                args += getPropOptions().toString();
            }
            return getFunctionName() + "(" + args + ")";
        }
    }
}
