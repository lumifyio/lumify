package io.lumify.palantir.service;

import io.lumify.palantir.model.PtGraph;
import io.lumify.palantir.model.PtUser;

public class PtGraphExporter extends ExporterBase<PtGraph> {
    public PtGraphExporter() {
        super(PtGraph.class);
    }

    @Override
    protected String getSql() {
        return "SELECT * FROM {namespace}.PT_GRAPH ORDER BY id";
    }
}
