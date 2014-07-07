package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkNotNull;

import org.securegraph.type.GeoCircle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

/**
 * A multi-column mapping that resolves GeoCircle instances from a columnar document.
 */
@JsonTypeName("geocircle")
@JsonPropertyOrder({ "latitudeColumn", "longitudeColumn", "radiusColumn" })
public class GeoCircleColumnValue implements ColumnValue<GeoCircle> {
    /**
     * The latitude column.
     */
    private final RequiredColumnValue<Double> latitudeColumn;

    /**
     * The longitude column.
     */
    private final RequiredColumnValue<Double> longitudeColumn;

    /**
     * The radius column.
     */
    private final RequiredColumnValue<Double> radiusColumn;

    /**
     * Create a new GeoCircleColumnValue.
     * @param latCol the latitude column
     * @param longCol the longitude column
     * @param radCol the radius column
     */
    public GeoCircleColumnValue(@JsonProperty("latitudeColumn") final ColumnValue<Double> latCol,
            @JsonProperty("longitudeColumn") final ColumnValue<Double> longCol,
            @JsonProperty("radiusColumn") final ColumnValue<Double> radCol) {
        checkNotNull(latCol, "latitude column is required");
        checkNotNull(longCol, "longitude column is required");
        checkNotNull(radCol, "radius column is required");
        this.latitudeColumn = new RequiredColumnValue<Double>(latCol);
        this.longitudeColumn = new RequiredColumnValue<Double>(longCol);
        this.radiusColumn = new RequiredColumnValue<Double>(radCol);
    }

    @JsonProperty("latitudeColumn")
    public final ColumnValue<Double> getLatitudeColumn() {
        return latitudeColumn.getDelegate();
    }

    @JsonProperty("longitudeColumn")
    public final ColumnValue<Double> getLongitudeColumn() {
        return longitudeColumn.getDelegate();
    }

    @JsonProperty("radiusColumn")
    public final ColumnValue<Double> getRadiusColumn() {
        return radiusColumn.getDelegate();
    }

    @Override
    public int getSortColumn() {
        return latitudeColumn.getSortColumn();
    }

    @Override
    public GeoCircle getValue(final List<String> row) {
        GeoCircle circle;
        try {
            Double latitude = latitudeColumn.getValue(row);
            Double longitude = longitudeColumn.getValue(row);
            Double radius = radiusColumn.getValue(row);
            circle = new GeoCircle(latitude, longitude, radius);
        } catch (IllegalArgumentException iae) {
            circle = null;
        }
        return circle;
    }

    @Override
    public int compareTo(final ColumnValue<?> o) {
        return latitudeColumn.compareTo(o);
    }
}
