package io.lumify.dbpedia.mapreduce.model;

import io.lumify.core.exception.LumifyException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineData {
    private static final Pattern LINE_PATTERN = Pattern.compile("^<(.*?)> <(.*?)> (.*) \\.$");

    private final String pageUrl;
    private final String propertyIri;
    private final String valueRaw;
    private final Value value;
    private final String pageTitle;

    public LineData(String pageUrl, String pageTitle, String propertyIri, String valueRaw, Value value) {
        this.pageUrl = pageUrl;
        this.pageTitle = pageTitle;
        this.propertyIri = propertyIri;
        this.valueRaw = valueRaw;
        this.value = value;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public String getPropertyIri() {
        return propertyIri;
    }

    public Value getValue() {
        return value;
    }

    // <http://dbpedia.org/resource/Autism> <http://dbpedia.org/ontology/diseasesdb> "1142"@en .
    public static LineData parse(String line) {
        Matcher m = LINE_PATTERN.matcher(line);
        if (!m.matches()) {
            throw new LumifyException("Could not find match for line: " + line);
        }

        String pageUrl = m.group(1);
        String propertyIri = m.group(2);
        String valueRaw = m.group(3);
        Value value = Value.parse(valueRaw);
        String pageTitle = parsePageTitleFromPageUrl(pageUrl);
        return new LineData(pageUrl, pageTitle, propertyIri, valueRaw, value);
    }

    public static String parsePageTitleFromPageUrl(String pageUrl) {
        int lastSlash = pageUrl.lastIndexOf('/');
        if (lastSlash < 0) {
            throw new LumifyException("Could not parse page title from page url: " + pageUrl);
        }
        String pageTitle = pageUrl.substring(lastSlash + 1);
        pageTitle = pageTitle.replace('_', ' ');
        return pageTitle;
    }
}
