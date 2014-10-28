package io.lumify.palantir.dataImport;

import io.lumify.core.exception.LumifyException;
import io.lumify.palantir.dataImport.model.PtPropertyAndValue;
import io.lumify.palantir.dataImport.model.PtPropertyType;
import org.securegraph.VertexBuilder;

public class PtPropertyAndValueImporter extends PtImporterBase<PtPropertyAndValue> {

    protected PtPropertyAndValueImporter(DataImporter dataImporter) {
        super(dataImporter, PtPropertyAndValue.class);
    }

    @Override
    protected void processRow(PtPropertyAndValue row) {
        PtPropertyType propertyType = getDataImporter().getPropertyTypes().get(row.getType());
        if (propertyType == null) {
            throw new LumifyException("Could not find property type: " + row.getType());
        }

        String propertyKey = getDataImporter().getIdPrefix() + row.getPropertyValueId();
        String propertyName = getPropertyName(propertyType.getUri());
        Object propertyValue = toValue(row.getValue(), propertyType);

        if (propertyValue == null) {
            // skip null values
        } else {
            VertexBuilder v = getDataImporter().getGraph().prepareVertex(getObjectId(row), getDataImporter().getVisibility());
            v.addPropertyValue(propertyKey, propertyName, propertyValue, getDataImporter().getVisibility());
            v.save(getDataImporter().getAuthorizations());
        }
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
                " FROM CLEATPR.PT_PROPERTY p, CLEATPR.PT_PROPERTY_VALUE pv" +
                " WHERE p.PROPERTY_VALUE_ID = pv.ID";
    }
}
