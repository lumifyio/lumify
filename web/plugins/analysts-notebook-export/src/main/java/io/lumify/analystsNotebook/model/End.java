package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class End {
    @JacksonXmlProperty(isAttribute = true)
    private int y;

    private Entity entity;

    private Label label;

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }
}
