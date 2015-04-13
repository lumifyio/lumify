package io.lumify.palantir.service;

import io.lumify.palantir.model.PtMediaAndValue;

public class PtMediaAndValueExporter extends ExporterBase<PtMediaAndValue> {
    public PtMediaAndValueExporter() {
        super(PtMediaAndValue.class);
    }

    @Override
    protected String getSql() {
        return "select m.ID" +
                ", m.REALM_ID" +
                ", m.LINK_OBJECT_ID" +
                ", m.DATA_EVENT_ID" +
                ", m.ORIGIN_DATA_EVENT_ID" +
                ", m.DELETED" +
                ", m.MEDIA_VALUE_ID" +
                ", m.CROSS_RESOLUTION_ID" +
                ", m.ACCESS_CONTROL_LIST_ID" +
                ", m.CREATED_BY" +
                ", m.TIME_CREATED" +
                ", m.LAST_MODIFIED_BY" +
                ", m.LAST_MODIFIED" +
                ", m.TITLE" +
                ", m.DESCRIPTION" +
                ", m.LINK_TYPE" +
                ", mv.TYPE" +
                ", mv.CONTENTS" +
                ", mv.CONTENTS_HASH" +
                " FROM {namespace}.PT_MEDIA m, {namespace}.PT_MEDIA_VALUE mv" +
                " WHERE " +
                "  m.MEDIA_VALUE_ID = mv.ID" +
                "  AND m.LINK_OBJECT_ID != -1" +
                " ORDER BY m.ID";
    }
}
