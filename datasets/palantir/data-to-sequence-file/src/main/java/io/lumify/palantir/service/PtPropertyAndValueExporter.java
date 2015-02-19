package io.lumify.palantir.service;

import io.lumify.palantir.model.PtPropertyAndValue;

public class PtPropertyAndValueExporter extends ExporterBase<PtPropertyAndValue> {
    public PtPropertyAndValueExporter() {
        super(PtPropertyAndValue.class);
    }

    @Override
    protected String getSql() {
        return "select p.ID" +
                ", p.REALM_ID" +
                ", p.LINK_OBJECT_ID" +
                ", p.DATA_EVENT_ID" +
                ", p.ORIGIN_DATA_EVENT_ID" +
                ", p.DELETED" +
                ", p.PROPERTY_VALUE_ID" +
                ", p.CROSS_RESOLUTION_ID" +
                ", p.ACCESS_CONTROL_LIST_ID" +
                ", p.LAST_MODIFIED_BY" +
                ", p.LAST_MODIFIED" +
                ", pv.TYPE" +
                ", pv.VALUE" +
                ", pv.LINK_ROLE_ID" +
                ", pv.LINK_TYPE" +
                ", pv.PRIORITY" +
                ", pv.USER_DISABLED_KEYWORD" +
                ", pv.CUSTOM_KEYWORD_TERM" +
                ", pv.GEOMETRY_XML" +
                ", pv.TIME_START" +
                ", pv.TIME_END" +
                ", pv.PROPERTY_STATUS" +
                ", pv.CREATED_BY" +
                ", pv.TIME_CREATED" +
                ", pv.GEOMETRY_GIS" +
                " FROM {namespace}.PT_PROPERTY p, {namespace}.PT_PROPERTY_VALUE pv" +
                " WHERE p.PROPERTY_VALUE_ID = pv.ID" +
                " ORDER BY p.ID";
    }
}
