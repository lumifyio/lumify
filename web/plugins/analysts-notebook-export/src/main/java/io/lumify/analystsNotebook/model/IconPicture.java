package io.lumify.analystsNotebook.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.lumify.analystsNotebook.AnalystsNotebookImageUtil;

public class IconPicture {
    @JacksonXmlProperty(isAttribute = true)
    private String data;

    @JacksonXmlProperty(isAttribute = true)
    private int dataLength;

    @JacksonXmlProperty(isAttribute = true)
    private boolean visible;

    public IconPicture() {

    }

    public IconPicture(byte[] data) {
        data = AnalystsNotebookImageUtil.convertImageFormat(data);
        this.data = AnalystsNotebookImageUtil.base64EncodedImageBytes(data);
        dataLength = data.length;
        visible = true;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
