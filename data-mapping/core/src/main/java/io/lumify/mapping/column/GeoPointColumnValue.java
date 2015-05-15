package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkNotNull;

import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import org.securegraph.type.GeoPoint;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

/**
 * A multi-column mapping that resolves GeoPoint instances from a columnar document.
 */
@JsonTypeName("geopoint")
@JsonPropertyOrder({ "latitudeColumn", "longitudeColumn", "altitudeColumn" })
public class GeoPointColumnValue extends AbstractColumnValue<GeoPoint> {
    /**
     * The latitude column.
     */
    private final RequiredColumnValue<Double> latitudeColumn;

    /**
     * The longitude column.
     */
    private final RequiredColumnValue<Double> longitudeColumn;

    /**
     * The altitude column.
     */
    private final RequiredColumnValue<Double> altitudeColumn;

    /**
     * Create a new GeoPointColumnValue.
     * @param latCol the latitude column
     * @param longCol the longitude column
     * @param altCol the altitude column
     * @param multiKey the optional multiKey
     */
    public GeoPointColumnValue(@JsonProperty("latitudeColumn") final ColumnValue<Double> latCol,
                               @JsonProperty("longitudeColumn") final ColumnValue<Double> longCol,
                               @JsonProperty(value="altitudeColumn", required=false) final ColumnValue<Double> altCol,
                               @JsonProperty(value="multiKey", required=false) final String multiKey) {
        super(multiKey);
        checkNotNull(latCol, "latitude column is required");
        checkNotNull(longCol, "longitude column is required");
        this.latitudeColumn = new RequiredColumnValue<Double>(latCol);
        this.longitudeColumn = new RequiredColumnValue<Double>(longCol);
        this.altitudeColumn = altCol != null ? new RequiredColumnValue<Double>(altCol) : null;
    }

    @JsonProperty("latitudeColumn")
    public final ColumnValue<Double> getLatitudeColumn() {
        return latitudeColumn.getDelegate();
    }

    @JsonProperty("longitudeColumn")
    public final ColumnValue<Double> getLongitudeColumn() {
        return longitudeColumn.getDelegate();
    }

    @JsonProperty("altitudeColumn")
    public final ColumnValue<Double> getAltitudeColumn() {
        return altitudeColumn != null ? altitudeColumn.getDelegate() : null;
    }

    @Override
    public int getSortColumn() {
        return latitudeColumn.getSortColumn();
    }

    @Override
    public GeoPoint getValue(final Row row) {
        GeoPoint point;
        try {
            Double latitude = latitudeColumn.getValue(row);
            Double longitude = longitudeColumn.getValue(row);
            Double altitude = altitudeColumn != null ? altitudeColumn.getValue(row) : null;
            point = new GeoPoint(latitude, longitude, altitude);
        } catch (IllegalArgumentException iae) {
            point = null;
        }
        return point;
    }
}
