package io.lumify.palantir.service;

import io.lumify.palantir.model.PtNodeDisplayType;

public class PtNodeDisplayTypeExporter extends OntologyTypeExporterBase<PtNodeDisplayType> {
    public PtNodeDisplayTypeExporter() {
        super(PtNodeDisplayType.class);
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_NODE_DISPLAY_TYPE";
    }
}
