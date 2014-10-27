package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.lumify.analystsNotebook.AnalystsNotebookVersion;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.workspace.WorkspaceEntity;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Direction;
import org.securegraph.Edge;
import org.securegraph.Vertex;

public class ChartItem {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ChartItem.class);

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

    public static ChartItem createEntity(AnalystsNotebookVersion version, String conceptType, String vertexId, String title, int x, int y) {
        IconStyle iconStyle = new IconStyle();
        iconStyle.setType(conceptType);

        Icon icon = new Icon();
        icon.setIconStyle(iconStyle);

        Entity entity = new Entity();
        entity.setEntityId(vertexId);
        entity.setIdentity(title);
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

        return chartItem;
    }

    public static ChartItem createFromVertexAndWorkspaceEntity(AnalystsNotebookVersion version, Vertex vertex, WorkspaceEntity workspaceEntity, OntologyRepository ontologyRepository) {
        String conceptType = LumifyProperties.CONCEPT_TYPE.getPropertyValue(vertex);
        Concept concept = ontologyRepository.getConceptByIRI(conceptType);
        String vertexId = vertex.getId();
        String title = LumifyProperties.TITLE.getPropertyValue(vertex);
        String titleFormula = concept.getTitleFormula();
        if (titleFormula != null) {
            LOGGER.debug("vertex has a titleFormula");
            // TODO: evaluate title formula
            // TODO: do we look for parent title formulas?
        }
        int x = workspaceEntity.getGraphPositionX();
        int y = workspaceEntity.getGraphPositionY();
        String imageUrl = LumifyProperties.ENTITY_IMAGE_URL.getPropertyValue(vertex);
        if (imageUrl != null) {
            LOGGER.debug("vertex has a an imageUrl");
            // TODO: find a place to provide an image URL
        } else {
            String imageVertexId = LumifyProperties.ENTITY_IMAGE_VERTEX_ID.getPropertyValue(vertex);
            if (imageVertexId != null) {
                LOGGER.debug("vertex has an entity image");
                // TODO: get thumbnail, see VertexThumbnail
                // TODO: find a way to embed image data
            } else {
                LOGGER.debug("vertex will use glyph icon (if we could put it in the XML)");
                byte[] glyphIcon = getGlyphIcon(concept, ontologyRepository);
                // TODO: find a way to embed image data
            }
        }
        // TODO: add other properties

        return createEntity(version, conceptType, vertexId, title, x, y);
    }

    private static byte[] getGlyphIcon(Concept concept, OntologyRepository ontologyRepository) {
        if (concept.hasGlyphIconResource()) {
            return concept.getGlyphIcon();
        } else {
            concept = ontologyRepository.getParentConcept(concept);
            if (concept != null) {
                return getGlyphIcon(concept, ontologyRepository);
            } else {
                return null;
            }
        }
    }

    public static ChartItem createLink(AnalystsNotebookVersion version, String label, String from, String to) {
        LinkStyle linkStyle = new LinkStyle();
        if (version == AnalystsNotebookVersion.VERSION_6) {
            linkStyle.setStrength(1);
        }
        // TODO: use directional arrow
        linkStyle.setArrowStyle(LinkStyle.ARROW_STYLE_ARROW_NONE);
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
