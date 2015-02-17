package io.lumify.palantir.service;

import io.lumify.palantir.model.PtObjectType;

public class PtObjectTypeExporter extends OntologyTypeExporterBase<PtObjectType> {
    public PtObjectTypeExporter() {
        super(PtObjectType.class);
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_OBJECT_TYPE";
    }
}
