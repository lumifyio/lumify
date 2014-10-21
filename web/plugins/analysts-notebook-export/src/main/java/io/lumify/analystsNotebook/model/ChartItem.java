package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.workspace.WorkspaceEntity;
import org.securegraph.Direction;
import org.securegraph.Edge;
import org.securegraph.Property;
import org.securegraph.Vertex;

public class ChartItem {
    @JacksonXmlProperty(isAttribute = true)
    private String label;

    @JacksonXmlProperty(isAttribute = true)
    private String description;

    @JacksonXmlProperty(isAttribute = true)
    private boolean dateSet;

    @JacksonXmlProperty(isAttribute = true)
    private int xPosition;

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

    public int getxPosition() {
        return xPosition;
    }

    public void setxPosition(int xPosition) {
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

    public static ChartItem createEntity(String conceptType, String vertexId, String title, int x, int y) {
        IconStyle iconStyle = new IconStyle();
        iconStyle.setType(conceptType);

        Icon icon = new Icon();
        icon.setIconStyle(iconStyle);

        Entity entity = new Entity();
        entity.setEntityId(vertexId);
        entity.setIdentity(title);
        entity.setIcon(icon);

        End end = new End();
        end.setY(y);
        end.setEntity(entity);

        ChartItem chartItem = new ChartItem();
        chartItem.setLabel(title);
        chartItem.setDateSet(false);
        chartItem.setxPosition(x);
        chartItem.setEnd(end);

        return chartItem;
    }

    public static ChartItem createFromVertexAndWorkspaceEntity(Vertex vertex, WorkspaceEntity workspaceEntity) {
        String conceptType = (String) vertex.getPropertyValue(LumifyProperties.CONCEPT_TYPE.getPropertyName());
        String vertexId = vertex.getId();
        String title = (String) vertex.getPropertyValue(LumifyProperties.TITLE.getPropertyName()); // TODO: use title formula
        int x = workspaceEntity.getGraphPositionX();
        int y = workspaceEntity.getGraphPositionY();

        return createEntity(conceptType, vertexId, title, x, y);
    }

    public static ChartItem createLink(String from, String to) {
        LinkStyle linkStyle = new LinkStyle();
        linkStyle.setArrowStyle(LinkStyle.ARROW_STYLE_ARROW_NONE);
        linkStyle.setType(LinkStyle.TYPE_LINK);

        Link link = new Link();
        link.setEnd1Id(from);
        link.setEnd2Id(to);
        link.setLinkStyle(linkStyle);

        ChartItem chartItem = new ChartItem();
        chartItem.setDateSet(false);
        chartItem.setLink(link);

        return chartItem;
    }

    public static ChartItem createFromEdge(Edge edge) {
        String from = edge.getVertexId(Direction.OUT);
        String to = edge.getVertexId(Direction.IN);

        return createLink(from, to);
    }

    public static ChartItem createLabel(String title, String labelId, int x, int y) {
        Label label = new Label();
        label.setLabelId(labelId);

        End end = new End();
        end.setY(y);
        end.setLabel(label);

        ChartItem chartItem = new ChartItem();
        chartItem.setLabel(title);
        chartItem.setDescription(title);
        chartItem.setDateSet(false);
        chartItem.setxPosition(x);
        chartItem.setEnd(end);

        return chartItem;
    }
}
