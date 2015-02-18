package io.lumify.palantir.service;

import io.lumify.palantir.model.PtLinkType;

public class PtLinkTypeExporter extends OntologyTypeExporterBase<PtLinkType> {
    public PtLinkTypeExporter() {
        super(PtLinkType.class);
    }

    @Override
    protected String getSql() {
        return "SELECT * FROM {namespace}.PT_LINK_TYPE ORDER BY type";
    }
}
