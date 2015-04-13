package io.lumify.palantir.service;

import io.lumify.palantir.model.PtObjectType;

public class PtObjectTypeExporter extends OntologyTypeExporterBase<PtObjectType> {
    public PtObjectTypeExporter() {
        super(PtObjectType.class);
    }

    @Override
    protected String getSql() {
        return "SELECT * FROM {namespace}.PT_OBJECT_TYPE ORDER BY type";
    }
}
