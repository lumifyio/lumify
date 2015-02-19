package io.lumify.palantir.service;

import io.lumify.palantir.model.PtObjectObject;

public class PtObjectObjectExporter extends ExporterBase<PtObjectObject> {
    public PtObjectObjectExporter() {
        super(PtObjectObject.class);
    }

    @Override
    protected String getSql() {
        return "SELECT * FROM {namespace}.PT_OBJECT_OBJECT ORDER BY link_id";
    }
}
