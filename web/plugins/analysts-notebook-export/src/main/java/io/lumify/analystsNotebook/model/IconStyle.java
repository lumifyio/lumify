package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IconStyle {
    @JacksonXmlProperty(isAttribute = true)
    private String type;

    @JacksonXmlProperty
    private IconPicture iconPicture;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public IconPicture getIconPicture() {
        return iconPicture;
    }

    public void setIconPicture(IconPicture iconPicture) {
        this.iconPicture = iconPicture;
    }
}
