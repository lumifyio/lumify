package io.lumify.dbpedia.mapreduce.model;

public class OtherValue extends Value {
    private final String value;
    private final String typeIri;

    public OtherValue(String value, String typeIri) {
        this.value = value;
        this.typeIri = typeIri;
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
