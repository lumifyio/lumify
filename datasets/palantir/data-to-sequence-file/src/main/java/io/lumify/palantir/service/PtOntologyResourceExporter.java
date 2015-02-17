package io.lumify.palantir.service;

import io.lumify.palantir.model.PtOntologyResource;

public class PtOntologyResourceExporter extends ExporterBase<PtOntologyResource> {
    public PtOntologyResourceExporter() {
        super(PtOntologyResource.class);
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_ONTOLOGY_RESOURCE";
    }
}
