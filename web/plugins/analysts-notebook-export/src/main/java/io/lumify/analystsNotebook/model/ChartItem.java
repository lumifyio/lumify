package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.lumify.analystsNotebook.AnalystsNotebookVersion;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.formula.FormulaEvaluator;
import io.lumify.core.model.artifactThumbnails.ArtifactThumbnail;
import io.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.workspace.WorkspaceEntity;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Authorizations;
import org.securegraph.Direction;
import org.securegraph.Edge;
import org.securegraph.Vertex;
import org.securegraph.property.StreamingPropertyValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ChartItem {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ChartItem.class);
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;

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
        if (version == AnalystsNotebookVersion.VERSION_6) {
            end.setX(x);
        }
        end.setY(y);
        end.setEntity(entity);

        ChartItem chartItem = new ChartItem();
        chartItem.setLabel(title);
        chartItem.setDateSet(false);
        if (version == AnalystsNotebookVersion.VERSION_7_OR_8) {
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
                                                               String baseUrl) {
        String conceptType = LumifyProperties.CONCEPT_TYPE.getPropertyValue(vertex);
        String vertexId = vertex.getId();
        String title = formulaEvaluator.evaluateTitleFormula(vertex, workspaceId, authorizations);
        int x = workspaceEntity.getGraphPositionX();
        int y = workspaceEntity.getGraphPositionY();

        List<Attribute> attributeCollection = Attribute.createCollectionFromVertex(vertex, ontologyRepository);

        String subtitle = formulaEvaluator.evaluateSubtitleFormula(vertex, workspaceId, authorizations);
        if (subtitle != null && subtitle.trim().length() > 0) {
            Attribute subtitleAttribute = new Attribute("subtitle", subtitle);
            attributeCollection.add(subtitleAttribute);
        }

        String time = formulaEvaluator.evaluateTimeFormula(vertex, workspaceId, authorizations);
        if (time != null && time.trim().length() > 0) {
            Attribute timeAttribute = new Attribute("time", time);
            attributeCollection.add(timeAttribute);
        }

        String imageUrl = LumifyProperties.ENTITY_IMAGE_URL.getPropertyValue(vertex);
        if (imageUrl != null) {
            Attribute imageUrlAttribute = new Attribute("imageUrl", imageUrl);
            attributeCollection.add(imageUrlAttribute);
        }

        String imageVertexId = LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyValue(vertex);
        IconPicture iconPicture = null;
        if (imageVertexId != null) {
            byte[] entityImage = getEntityImage(vertex, artifactThumbnailRepository, user);
            if (entityImage != null) {
                // TODO: broken in 8.5.1
                iconPicture = new IconPicture(entityImage);
            }
        }

        return createEntity(version, conceptType, vertexId, title, x, y, iconPicture, attributeCollection, baseUrl, workspaceId);
    }

    private static byte[] getEntityImage(Vertex vertex, ArtifactThumbnailRepository artifactThumbnailRepository, User user) {
        int[] dimensions = new int[]{THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT};
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

    public static ChartItem createLink(AnalystsNotebookVersion version, String label, String from, String to) {
        LinkStyle linkStyle = new LinkStyle();
        if (version == AnalystsNotebookVersion.VERSION_6) {
            linkStyle.setStrength(1);
        }
        linkStyle.setArrowStyle(LinkStyle.ARROW_STYLE_ARROW_ON_HEAD);
        linkStyle.setType(LinkStyle.TYPE_LINK);

        Link link = new Link();
        link.setEnd1Id(from);
        link.setEnd2Id(to);
        link.setLinkStyle(linkStyle);

        ChartItem chartItem = new ChartItem();
        chartItem.setLabel(label);
        chartItem.setDateSet(false);
        chartItem.setLink(link);

        return chartItem;
    }

    public static ChartItem createFromEdge(AnalystsNotebookVersion version, Edge edge, OntologyRepository ontologyRepository) {
        String label = ontologyRepository.getDisplayNameForLabel(edge.getLabel());
        String from = edge.getVertexId(Direction.OUT);
        String to = edge.getVertexId(Direction.IN);

        return createLink(version, label, from, to);
    }
}
