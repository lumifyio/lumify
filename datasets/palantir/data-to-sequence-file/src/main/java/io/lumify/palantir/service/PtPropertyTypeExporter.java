package io.lumify.palantir.service;

import io.lumify.palantir.model.PtPropertyType;

public class PtPropertyTypeExporter extends OntologyTypeExporterBase<PtPropertyType> {
    public PtPropertyTypeExporter() {
        super(PtPropertyType.class);
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_PROPERTY_TYPE";
    }
}
