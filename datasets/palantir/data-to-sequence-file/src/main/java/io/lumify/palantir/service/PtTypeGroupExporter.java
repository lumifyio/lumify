package io.lumify.palantir.service;

import io.lumify.palantir.model.PtTypeGroup;

public class PtTypeGroupExporter extends ExporterBase<PtTypeGroup> {
    public PtTypeGroupExporter() {
        super(PtTypeGroup.class);
    }

    @Override
    protected String getSql() {
        return "select TYPE," +
                "CONFIG," +
                "HIDDEN," +
                "CREATED_BY," +
                "TIME_CREATED," +
                "LAST_MODIFIED" +
                " FROM {namespace}.PT_TYPE_GROUP";
    }
}
