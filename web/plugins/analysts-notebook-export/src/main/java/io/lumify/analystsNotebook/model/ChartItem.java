package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.lumify.analystsNotebook.AnalystsNotebookExportConfiguration;
import io.lumify.analystsNotebook.AnalystsNotebookFeature;
import io.lumify.analystsNotebook.AnalystsNotebookVersion;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.formula.FormulaEvaluator;
import io.lumify.core.model.artifactThumbnails.ArtifactThumbnail;
import io.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.workspace.WorkspaceEntity;
import io.lumify.core.user.User;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.securegraph.Authorizations;
import org.securegraph.Direction;
import org.securegraph.Edge;
import org.securegraph.Vertex;
import org.securegraph.property.StreamingPropertyValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ChartItem {
    @JacksonXmlProperty(isAttribute = true)
    private String label;

    @JacksonXmlProperty(isAttribute = true)
    private String description;

    @JacksonXmlProperty(isAttribute = true)
    private boolean dateSet;

    // located in End for version 6
    @JacksonXmlProperty(isAttribute = true)
    private Integer xPosition;

    private End end;

    private Link link;

    @JacksonXmlProperty(isAttribute = true)
    private String sourceReference;

    @JacksonXmlElementWrapper(localName = "AttributeCollection")
    @JacksonXmlProperty(localName = "Attribute")
    private List<Attribute> attributeCollection;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDateSet() {
        return dateSet;
    }

    public void setDateSet(boolean dateSet) {
        this.dateSet = dateSet;
    }

    public Integer getxPosition() {
        return xPosition;
    }

    public void setxPosition(Integer xPosition) {
        this.xPosition = xPosition;
    }

    public End getEnd() {
        return end;
    }

    public void setEnd(End end) {
        this.end = end;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public List<Attribute> getAttributeCollection() {
        return attributeCollection;
    }

    public void setAttributeCollection(List<Attribute> attributeCollection) {
        this.attributeCollection = attributeCollection;
    }

    public static ChartItem createEntity(AnalystsNotebookVersion version,
                                         String conceptType,
                                         String vertexId,
                                         String title,
                                         int x,
                                         int y,
                                         IconPicture iconPicture,
                                         List<Attribute> attributeCollection,
                                         String baseUrl,
                                         String workspaceId) {
        IconStyle iconStyle = new IconStyle();
        iconStyle.setType(conceptType);
        if (iconPicture != null) {
            iconStyle.setIconPicture(iconPicture);
        }

        Icon icon = new Icon();
        icon.setIconStyle(iconStyle);

        Entity entity = new Entity();
        entity.setEntityId(vertexId);
        entity.setIdentity(vertexId);
        entity.setIcon(icon);

        End end = new End();
        if (version.supports(AnalystsNotebookFeature.END_X)) {
            end.setX(x);
        }
        end.setY(y);
        end.setEntity(entity);

        ChartItem chartItem = new ChartItem();
        chartItem.setLabel(title);
        chartItem.setDateSet(false);
        if (version.supports(AnalystsNotebookFeature.CHART_ITEM_X_POSITION)) {
            chartItem.setxPosition(x);
        }
        chartItem.setEnd(end);

        chartItem.setSourceReference(String.format("%s/#v=%s&w=%s", baseUrl, vertexId, workspaceId));

        chartItem.setAttributeCollection(attributeCollection);

        return chartItem;
    }

    public static ChartItem createFromVertexAndWorkspaceEntity(AnalystsNotebookVersion version,
                                                               Vertex vertex,
                                                               WorkspaceEntity workspaceEntity,
                                                               OntologyRepository ontologyRepository,
                                                               ArtifactThumbnailRepository artifactThumbnailRepository,
                                                               FormulaEvaluator formulaEvaluator,
                                                               String workspaceId,
                                                               Authorizations authorizations,
                                                               User user,
                                                               String baseUrl,
                                                               AnalystsNotebookExportConfiguration analystsNotebookExportConfiguration) {
        String conceptType = LumifyProperties.CONCEPT_TYPE.getPropertyValue(vertex);
        String vertexId = vertex.getId();
        String title = formulaEvaluator.evaluateTitleFormula(vertex, workspaceId, authorizations);
        int x = workspaceEntity.getGraphPositionX();
        int y = workspaceEntity.getGraphPositionY();

        List<Attribute> attributeCollection = new ArrayList<Attribute>();
        if (analystsNotebookExportConfiguration.includeProperties()) {
            attributeCollection.addAll(Attribute.createCollectionFromVertex(vertex, ontologyRepository));
        }
        if (analystsNotebookExportConfiguration.includeVisibility()) {
            VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(vertex);
            if (visibilityJson != null) {
                String visibilitySource = visibilityJson.getSource();
                if (visibilitySource != null && visibilitySource.trim().length() > 0) {
                    String label = analystsNotebookExportConfiguration.getVisibilityLabel();
                    Attribute visibilityAttribute = new Attribute(label, visibilitySource);
                    attributeCollection.add(visibilityAttribute);
                }
            }
        }
        if (analystsNotebookExportConfiguration.includeSubtitle()) {
            String subtitle = formulaEvaluator.evaluateSubtitleFormula(vertex, workspaceId, authorizations);
            if (subtitle != null && subtitle.trim().length() > 0) {
                Attribute subtitleAttribute = new Attribute(AttributeClass.NAME_SUBTITLE, subtitle);
                attributeCollection.add(subtitleAttribute);
            }
        }
        if (analystsNotebookExportConfiguration.includeTime()) {
            String time = formulaEvaluator.evaluateTimeFormula(vertex, workspaceId, authorizations);
            if (time != null && time.trim().length() > 0) {
                Attribute timeAttribute = new Attribute(AttributeClass.NAME_TIME, time);
                attributeCollection.add(timeAttribute);
            }
        }
        if (analystsNotebookExportConfiguration.includeImageUrl()) {
            String imageUrl = LumifyProperties.ENTITY_IMAGE_URL.getPropertyValue(vertex);
            if (imageUrl != null) {
                Attribute imageUrlAttribute = new Attribute(AttributeClass.NAME_IMAGE_URL, imageUrl);
                attributeCollection.add(imageUrlAttribute);
            }
        }

        IconPicture iconPicture = null;
        if (version.supports(AnalystsNotebookFeature.ICON_PICTURE) && analystsNotebookExportConfiguration.enableIconPicture()) {
            String mimeType = LumifyProperties.MIME_TYPE.getPropertyValue(vertex);
            if (mimeType != null && mimeType.toLowerCase().startsWith("image/")) {
                iconPicture = new IconPicture(getThumbnailBytes(vertex, artifactThumbnailRepository, user, analystsNotebookExportConfiguration));
            } else {
                String imageVertexId = LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyValue(vertex);
                if (imageVertexId != null) {
                    Vertex imageVertex = vertex.getGraph().getVertex(imageVertexId, authorizations);
                    iconPicture = new IconPicture(getThumbnailBytes(imageVertex, artifactThumbnailRepository, user, analystsNotebookExportConfiguration));
                }
            }
        }

        return createEntity(version, conceptType, vertexId, title, x, y, iconPicture, attributeCollection, baseUrl, workspaceId);
    }

    private static byte[] getThumbnailBytes(Vertex vertex, ArtifactThumbnailRepository artifactThumbnailRepository, User user, AnalystsNotebookExportConfiguration analystsNotebookExportConfiguration) {
        int[] dimensions = new int[]{analystsNotebookExportConfiguration.getThumbnailWidth(), analystsNotebookExportConfiguration.getThumbnailHeight()};
        String type = "raw";

        ArtifactThumbnail thumbnail = artifactThumbnailRepository.getThumbnail(vertex.getId(), type, dimensions[0], dimensions[1], user);
        if (thumbnail == null) {
            StreamingPropertyValue rawPropertyValue = LumifyProperties.RAW.getPropertyValue(vertex);
            if (rawPropertyValue == null) {
                return null;
            }
            InputStream in = rawPropertyValue.getInputStream();
            try {
                thumbnail = artifactThumbnailRepository.createThumbnail(vertex, type, in, dimensions, user);
            } catch (IOException e) {
                throw new LumifyException("error creating thumbnail", e);
            }
        }

        return thumbnail.getThumbnailData();
    }

    public static ChartItem createFromEdge(AnalystsNotebookVersion version, Edge edge, OntologyRepository ontologyRepository) {
        String label = ontologyRepository.getDisplayNameForLabel(edge.getLabel());
        String from = edge.getVertexId(Direction.OUT);
        String to = edge.getVertexId(Direction.IN);

        return createLink(version, label, from, to);
    }

    private static ChartItem createLink(AnalystsNotebookVersion version, String chartItemLabel, String from, String to) {
        LinkStyle linkStyle = new LinkStyle();
        if (version.supports(AnalystsNotebookFeature.LINK_STYLE_STRENGTH)) {
            linkStyle.setStrength(1);
        }
        linkStyle.setArrowStyle(LinkStyle.ARROW_STYLE_ARROW_ON_HEAD);
        linkStyle.setType(LinkStyle.TYPE_LINK);

        Link link = new Link();
        link.setEnd1Id(from);
        link.setEnd2Id(to);
        link.setLinkStyle(linkStyle);

        ChartItem chartItem = new ChartItem();
        chartItem.setLabel(chartItemLabel);
        chartItem.setDateSet(false);
        chartItem.setLink(link);

        return chartItem;
    }

    public static ChartItem createLabel(AnalystsNotebookVersion version, int x, int y, String chartItemLabel, String chartItemDescription, String labelId) {
        Label label = new Label();
        label.setLabelId(labelId);

        End end = new End();
        if (version.supports(AnalystsNotebookFeature.END_X)) {
            end.setX(x);
        }
        end.setY(y);
        end.setLabel(label);

        ChartItem chartItem = new ChartItem();
        chartItem.setLabel(chartItemLabel);
        chartItem.setDescription(chartItemDescription);
        chartItem.setDateSet(false);
        if (version.supports(AnalystsNotebookFeature.CHART_ITEM_X_POSITION)) {
            chartItem.setxPosition(x);
        }
        chartItem.setEnd(end);

        return chartItem;
    }
}
