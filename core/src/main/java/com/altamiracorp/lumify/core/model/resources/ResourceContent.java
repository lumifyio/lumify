package com.altamiracorp.lumify.core.model.resources;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ResourceContent extends ColumnFamily {
    public static final String NAME = "Content";
    private static final String DATA = "data";
    private static final String CONTENT_TYPE = "contentType";

    public ResourceContent() {
        super(NAME);
    }

    public ResourceContent setData(byte[] data) {
        set(DATA, data);
        return this;
    }

    public byte[] getData() {
        return Value.toBytes(get(DATA));
    }

    public ResourceContent setContentType(String contentType) {
        set(CONTENT_TYPE, contentType);
        return this;
    }

    public String getContentType() {
        return Value.toString(get(CONTENT_TYPE));
    }

    public BufferedImage getDataImage() {
        byte[] data = getData();
        if (data == null) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException ex) {
            throw new RuntimeException("Could not load image", ex);
        }
    }
}
