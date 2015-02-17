package io.lumify.palantir.service;

import io.lumify.palantir.model.PtUser;

public class PtUserExporter extends ExporterBase<PtUser> {
    public PtUserExporter() {
        super(PtUser.class);
    }

    @Override
    protected String getSql() {
        return "select * FROM {namespace}.PT_USER";
    }
}
