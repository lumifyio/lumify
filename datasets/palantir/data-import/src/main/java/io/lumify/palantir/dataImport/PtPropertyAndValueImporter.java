package io.lumify.palantir.dataImport;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.palantir.dataImport.model.PtPropertyAndValue;
import io.lumify.palantir.dataImport.model.PtPropertyType;
import io.lumify.palantir.dataImport.util.JGeometryWrapper;
import org.securegraph.VertexBuilder;
import org.securegraph.type.GeoPoint;

import java.awt.geom.Point2D;

public class PtPropertyAndValueImporter extends PtRowImporterBase<PtPropertyAndValue> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PtPropertyAndValueImporter.class);

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
        Object propertyValue = propertyType.getDisplayFormula().toValue(row.getValue());
        propertyValue = toValueGeo(row, propertyValue);

        if (propertyValue == null) {
            // skip null values
        } else {
            VertexBuilder v = getDataImporter().getGraph().prepareVertex(getObjectId(row), getDataImporter().getVisibility());
            v.addPropertyValue(propertyKey, propertyName, propertyValue, getDataImporter().getVisibility());
            v.save(getDataImporter().getAuthorizations());
        }
    }

    private Object toValueGeo(PtPropertyAndValue row, Object propertyValue) {
        JGeometryWrapper geometryGis = JGeometryWrapper.load(row.getGeometryGis());
        if (geometryGis == null) {
            return propertyValue;
        }
        if (geometryGis.getType() == JGeometryWrapper.Type.POINT) {
            if (propertyValue == null) {
                propertyValue = "";
            }
            Point2D pt = geometryGis.getJavaPoint();
            double lon = pt.getX();
            double lat = pt.getY();
            propertyValue = new GeoPoint(lat, lon, propertyValue.toString());
        } else {
            LOGGER.error("Unhandled geometry gis type: " + geometryGis.getType());
        }
        return propertyValue;
    }

    protected String getObjectId(PtPropertyAndValue ptPropertyAndValue) {
        return getDataImporter().getIdPrefix() + ptPropertyAndValue.getLinkObjectId();
    }

    protected String getPropertyName(String uri) {
        return getDataImporter().getOwlPrefix() + uri;
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
                " WHERE p.PROPERTY_VALUE_ID = pv.ID";
    }
}
