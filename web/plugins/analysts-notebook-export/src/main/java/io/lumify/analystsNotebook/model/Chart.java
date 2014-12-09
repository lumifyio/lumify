package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.lumify.core.exception.LumifyException;

import java.util.List;

@JacksonXmlRootElement
public class Chart {
    private static final String XML_DECLARATION = "<?xml version='1.0' encoding='UTF-8'?>";
    private static final String XML_COMMENT_START = "<!-- ";
    private static final String XML_COMMENT_END = " -->";
    private static final String XML_COMMENT_INDENT = "     ";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

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

    public String toXml(List<String> comments) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(XML_DECLARATION).append(LINE_SEPARATOR);
            sb = appendComments(sb, comments);
            sb.append(getXmlMapper().writeValueAsString(this));
            return sb.toString();
        } catch (JsonProcessingException e) {
            throw new LumifyException("exception while generating XML", e);
        }
    }

    private static StringBuilder appendComments(StringBuilder sb, List<String> comments) {
        if (comments != null && comments.size() > 0) {
            if (comments.size() == 1) {
                return sb.append(XML_COMMENT_START).append(comments.get(0)).append(XML_COMMENT_END).append(LINE_SEPARATOR);
            } else {
                for (int i = 0; i < comments.size(); i++) {
                    sb.append(i == 0 ? XML_COMMENT_START : XML_COMMENT_INDENT).append(comments.get(i)).append(LINE_SEPARATOR);
                }
                sb.append(XML_COMMENT_END).append(LINE_SEPARATOR);
            }
        }
        return sb;
    }

    private static XmlMapper getXmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
