package io.lumify.gpw;

import io.lumify.core.config.Configurable;

public class MediaPropertyConfiguration {
    public static final String PROPERTY_NAME_PREFIX = "ontology.iri.media";

    public String yAxisFlippedIri;
    public String clockwiseRotationIri;
    public String geoLocationIri;
    public String headingIri;
    public String dateTakenIri;
    public String deviceMakeIri;
    public String deviceModelIri;
    public String widthIri;
    public String heightIri;
    public String durationIri;
    public String fileSizeIri;
    public String metadataIri;

    @Configurable(name = "yAxisFlipped", required = false)
    public void setYAxisFlippedIri(String yAxisFlippedIri) {
        this.yAxisFlippedIri = yAxisFlippedIri;
    }

    @Configurable(name = "clockwiseRotation", required = false)
    public void setClockwiseRotationIri(String clockwiseRotationIri) {
        this.clockwiseRotationIri = clockwiseRotationIri;
    }

    @Configurable(name = "geoLocation", required = false)
    public void setGeoLocationIri(String geoLocationIri) {
        this.geoLocationIri = geoLocationIri;
    }

    @Configurable(name = "imageHeading", required = false)
    public void setHeadingIri(String headingIri) {
        this.headingIri = headingIri;
    }

    @Configurable(name = "dateTaken", required = false)
    public void setDateTakenIri(String dateTakenIri) {
        this.dateTakenIri = dateTakenIri;
    }

    @Configurable(name = "deviceMake", required = false)
    public void setDeviceMakeIri(String deviceMakeIri) {
        this.deviceMakeIri = deviceMakeIri;
    }

    @Configurable(name = "deviceModel", required = false)
    public void setDeviceModelIri(String deviceModelIri) {
        this.deviceModelIri = deviceModelIri;
    }

    @Configurable(name = "width", required = false)
    public void setWidthIri(String widthIri) {
        this.widthIri = widthIri;
    }

    @Configurable(name = "height", required = false)
    public void setHeightIri(String heightIri) {
        this.heightIri = heightIri;
    }

    @Configurable(name = "duration", required = false)
    public void setDurationIri(String durationIri) {
        this.durationIri = durationIri;
    }

    @Configurable(name = "fileSize", required = false)
    public void setFileSizeIri(String fileSizeIri) {
        this.fileSizeIri = fileSizeIri;
    }

    @Configurable(name = "metadata", required = false)
    public void setMetadataIri(String metadataIri) {
        this.metadataIri = metadataIri;
    }
}
