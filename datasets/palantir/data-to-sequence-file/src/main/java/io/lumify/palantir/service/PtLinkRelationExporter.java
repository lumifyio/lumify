package io.lumify.palantir.service;

import io.lumify.palantir.model.PtLinkRelation;

public class PtLinkRelationExporter extends ExporterBase<PtLinkRelation> {
    private StringBuilder xml;

    public PtLinkRelationExporter() {
        super(PtLinkRelation.class);
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_LINK_RELATION";
    }
}
