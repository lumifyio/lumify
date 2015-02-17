package io.lumify.palantir.service;

import io.lumify.palantir.model.PtImageInfo;

public class PtImageInfoExporter extends ExporterBase<PtImageInfo> {
    public PtImageInfoExporter() {
        super(PtImageInfo.class);
    }

    @Override
    protected String getSql() {
        return "select * from {namespace}.PT_IMAGE_INFO";
    }
}
