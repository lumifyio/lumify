package io.lumify.storm;

import io.lumify.core.config.Configurable;

public class MediaPropertyConfiguration {
    public static final String PROPERTY_NAME_PREFIX = "ontology.iri.media";

    private String yAxisFlippedIri;
    private String clockwiseRotationIri;
    private String geoLocationIri;
    private String headingIri;
    private String dateTakenIri;
    private String deviceMakeIri;
    private String deviceModelIri;
    private String widthIri;
    private String heightIri;
    private String durationIri;
    private String fileSizeIri;
    private String metadataIri;

    @Configurable(name = "yAxisFlippedIri", required = false)
    public void setYAxisFlippedIri(String yAxisFlippedIri) {
        this.yAxisFlippedIri = yAxisFlippedIri;
    }

    @Configurable(name = "clockwiseRotationIri", required = false)
    public void setClockwiseRotationIri(String clockwiseRotationIri) { this.clockwiseRotationIri = clockwiseRotationIri; }

    @Configurable(name = "geoLocationIri", required = false)
    public void setGeoLocationIri(String geoLocationIri) { this.geoLocationIri = geoLocationIri; }

    @Configurable(name = "headingIri", required = false)
    public void setHeadingIri(String headingIri) { this.headingIri = headingIri; }

    @Configurable(name = "dateTakenIri", required = false)
    public void setDateTakenIri(String dateTakenIri) { this.dateTakenIri = dateTakenIri; }

    @Configurable(name = "deviceMakeIri", required = false)
    public void setDeviceMakeIri(String deviceMakeIri) { this.deviceMakeIri = deviceMakeIri; }

    @Configurable(name = "deviceModelIri", required = false)
    public void setDeviceModelIri(String deviceModelIri) { this.deviceModelIri = deviceModelIri; }

    @Configurable(name = "widthIri", required = false)
    public void setWidthIri(String widthIri) { this.widthIri = widthIri; }

    @Configurable(name = "heightIri", required = false)
    public void setHeightIri(String heightIri) { this.heightIri = heightIri; }

    @Configurable(name = "durationIri", required = false)
    public void setDurationIri(String durationIri) { this.durationIri = durationIri; }

    @Configurable(name = "fileSizeIri", required = false)
    public void setFileSizeIri(String fileSizeIri) { this.fileSizeIri = fileSizeIri; }

    @Configurable(name = "metadataIri", required = false)
    public void setMetadataIri(String metadataIri) { this.metadataIri = metadataIri; }

    public String getDurationIri() {
        return durationIri;
    }

    public String getYAxisFlippedIri() {
        return yAxisFlippedIri;
    }

    public String getClockwiseRotationIri() {
        return clockwiseRotationIri;
    }

    public String getGeoLocationIri() {
        return geoLocationIri;
    }

    public String getHeadingIri() {
        return headingIri;
    }

    public String getDateTakenIri() {
        return dateTakenIri;
    }

    public String getDeviceMakeIri() {
        return deviceMakeIri;
    }

    public String getDeviceModelIri() {
        return deviceModelIri;
    }

    public String getWidthIri() {
        return widthIri;
    }

    public String getHeightIri() {
        return heightIri;
    }

    public String getFileSizeIri() {
        return fileSizeIri;
    }

    public String getMetadataIri() {
        return metadataIri;
    }
}
