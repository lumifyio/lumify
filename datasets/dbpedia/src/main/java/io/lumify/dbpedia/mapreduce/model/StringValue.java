package io.lumify.dbpedia.mapreduce.model;

public class StringValue extends Value {
    private final String value;
    private final String language;

    public StringValue(String value, String language) {
        this.value = value;
        this.language = language;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getValueString() {
        return value;
    }
}
