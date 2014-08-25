package io.lumify.storm;

import io.lumify.core.config.Configurable;

public class MediaPropertyConfiguration {
    private String durationIri;
    private String videoRotationIri;
    private String yAxisFlipNeededIri;
    private String cwRotationNeededIri;
    private String headingIri;
    private String dateTakenIri;
    private String deviceMakeIri;
    private String deviceModelIri;
    private String widthIri;
    private String heightIri;
    private String fileSizeIri;
    private String metadataIri;
    private String geoLocationIri;

    @Configurable(name = "durationIri", required = false)
    public void setDurationIri(String durationIri) { this.durationIri = durationIri; }

    @Configurable(name = "videoRotationIri", required = false)
    public void setVideoRotationIri(String videoRotationIri) { this.videoRotationIri = videoRotationIri; }

    @Configurable(name = "yAxisFlipNeededIri", required = false)
    public void setYAxisFlipNeededIri(String yAxisFlipNeededIri) {
        this.yAxisFlipNeededIri = yAxisFlipNeededIri;
    }

    @Configurable(name = "cwRotationNeededIri", required = false)
    public void setCwRotationNeededIri(String cwRotationNeededIri) { this.cwRotationNeededIri = cwRotationNeededIri; }

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

    @Configurable(name = "fileSizeIri", required = false)
    public void setFileSizeIri(String fileSizeIri) { this.fileSizeIri = fileSizeIri; }

    @Configurable(name = "metadataIri", required = false)
    public void setMetadataIri(String metadataIri) { this.metadataIri = metadataIri; }

    @Configurable(name = "geoLocationIri", required = false)
    public void setGeoLocationIri(String geoLocationIri) { this.geoLocationIri = geoLocationIri; }

    public String getDurationIri() {
        return durationIri;
    }

    public String getVideoRotationIri() {
        return videoRotationIri;
    }

    public String getyAxisFlipNeededIri() {
        return yAxisFlipNeededIri;
    }

    public String getCwRotationNeededIri() {
        return cwRotationNeededIri;
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

    public String getGeoLocationIri() {
        return geoLocationIri;
    }
}
