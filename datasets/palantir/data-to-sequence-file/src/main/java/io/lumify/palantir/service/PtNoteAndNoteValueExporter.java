package io.lumify.palantir.service;

import io.lumify.palantir.model.PtNoteAndNoteValue;

public class PtNoteAndNoteValueExporter extends ExporterBase<PtNoteAndNoteValue> {
    public PtNoteAndNoteValueExporter() {
        super(PtNoteAndNoteValue.class);
    }

    @Override
    protected String getSql() {
        return "SELECT" +
                "  n.ID," +
                "  n.REALM_ID," +
                "  n.LINK_OBJECT_ID," +
                "  n.DATA_EVENT_ID," +
                "  n.ORIGIN_DATA_EVENT_ID," +
                "  n.DELETED," +
                "  n.NOTE_VALUE_ID," +
                "  n.CROSS_RESOLUTION_ID," +
                "  n.ACCESS_CONTROL_LIST_ID," +
                "  n.CREATED_BY," +
                "  n.TIME_CREATED," +
                "  n.LAST_MODIFIED_BY," +
                "  n.LAST_MODIFIED," +
                "  nv.TITLE," +
                "  nv.CONTENTS," +
                "  nv.LINK_ROLE_ID," +
                "  nv.LINK_TYPE," +
                "  nv.NOTE_OFFSET" +
                " FROM {namespace}.PT_NOTE n, {namespace}.PT_NOTE_VALUE nv" +
                " WHERE n.NOTE_VALUE_ID=nv.ID" +
                " ORDER BY n.ID";
    }
}
