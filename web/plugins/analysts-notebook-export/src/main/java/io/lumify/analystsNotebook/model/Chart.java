package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement
public class Chart {
    @JacksonXmlProperty(isAttribute = true)
    private boolean idReferenceLinking;

    @JacksonXmlProperty(isAttribute = true)
    private boolean rigorous;

    @JacksonXmlElementWrapper(localName = "AttributeClassCollection")
    @JacksonXmlProperty(localName = "AttributeClass")
    private List<AttributeClass> attributeClassCollection;

    @JacksonXmlElementWrapper(localName = "LinkTypeCollection")
    @JacksonXmlProperty(localName = "LinkType")
    private List<LinkType> linkTypeCollection;

    @JacksonXmlElementWrapper(localName = "EntityTypeCollection")
    @JacksonXmlProperty(localName = "EntityType")
    private List<EntityType> entityTypeCollection;

    @JacksonXmlElementWrapper(localName = "CustomImageCollection")
    @JacksonXmlProperty(localName = "CustomImage")
    private List<CustomImage> customImageCollection;

    @JacksonXmlElementWrapper(localName = "ChartItemCollection")
    @JacksonXmlProperty(localName = "ChartItem")
    private List<ChartItem> chartItemCollection;

    private Summary summary;

    private PrintSettings printSettings;

    public boolean isIdReferenceLinking() {
        return idReferenceLinking;
    }

    public void setIdReferenceLinking(boolean idReferenceLinking) {
        this.idReferenceLinking = idReferenceLinking;
    }

    public boolean isRigorous() {
        return rigorous;
    }

    public void setRigorous(boolean rigorous) {
        this.rigorous = rigorous;
    }

    public List<AttributeClass> getAttributeClassCollection() {
        return attributeClassCollection;
    }

    public void setAttributeClassCollection(List<AttributeClass> attributeClassCollection) {
        this.attributeClassCollection = attributeClassCollection;
    }

    public List<LinkType> getLinkTypeCollection() {
        return linkTypeCollection;
    }

    public void setLinkTypeCollection(List<LinkType> linkTypeCollection) {
        this.linkTypeCollection = linkTypeCollection;
    }

    public List<EntityType> getEntityTypeCollection() {
        return entityTypeCollection;
    }

    public void setEntityTypeCollection(List<EntityType> entityTypeCollection) {
        this.entityTypeCollection = entityTypeCollection;
    }

    public List<CustomImage> getCustomImageCollection() {
        return customImageCollection;
    }

    public void setCustomImageCollection(List<CustomImage> customImageCollection) {
        this.customImageCollection = customImageCollection;
    }

    public List<ChartItem> getChartItemCollection() {
        return chartItemCollection;
    }

    public void setChartItemCollection(List<ChartItem> chartItemCollection) {
        this.chartItemCollection = chartItemCollection;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public PrintSettings getPrintSettings() {
        return printSettings;
    }

    public void setPrintSettings(PrintSettings printSettings) {
        this.printSettings = printSettings;
    }
}
