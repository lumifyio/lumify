package io.lumify.dbpedia.mapreduce.model;

import io.lumify.core.exception.LumifyException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Value {
    private static final Pattern STRING_VALUE_PATTERN = Pattern.compile("^\"(.*?)\"@(.*)$");
    private static final Pattern LINK_VALUE_PATTERN = Pattern.compile("^<(.*?)>$");
    private static final Pattern OTHER_VALUE_PATTERN = Pattern.compile("^\"(.*?)\"\\^\\^<(.*)>$");

    public abstract Object getValue();

    public abstract String getValueString();

    public static Value parse(String valueRaw) {
        Matcher m = STRING_VALUE_PATTERN.matcher(valueRaw);
        if (m.matches()) {
            return new StringValue(m.group(1), m.group(2));
        }

        m = LINK_VALUE_PATTERN.matcher(valueRaw);
        if (m.matches()) {
            return new LinkValue(m.group(1));
        }

        m = OTHER_VALUE_PATTERN.matcher(valueRaw);
        if (m.matches()) {
            return new OtherValue(m.group(1), m.group(2));
        }

        throw new LumifyException("Could not parse value: " + valueRaw);
    }
}
