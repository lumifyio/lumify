package io.lumify.core.model.artifactThumbnails;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BigTableArtifactThumbnailMetadata extends ColumnFamily {
    public static final String NAME = "metadata";
    private static final String DATA = "data";
    private static final String TYPE = "type";
    private static final String FORMAT = "format";

    public BigTableArtifactThumbnailMetadata() {
        super(NAME);
    }

    public byte[] getData() {
        return Value.toBytes(get(DATA));
    }

    public BigTableArtifactThumbnailMetadata setData(byte[] data) {
        set(DATA, data);
        return this;
    }

    public int getType() {
        return Value.toInteger(get(TYPE));
    }

    public BigTableArtifactThumbnailMetadata setType(int type) {
        set(TYPE, type);
        return this;
    }

    public String getFormat() {
        return Value.toString(get(FORMAT));
    }

    public BigTableArtifactThumbnailMetadata setFormat(String format) {
        set(FORMAT, format);
        return this;
    }

    public BufferedImage getImage() {
        try {
            byte[] data = getData();
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            throw new RuntimeException("Could not load image", e);
        }
    }
}
