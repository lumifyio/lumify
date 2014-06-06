package io.lumify.core.ingest.video;

import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.model.properties.RawLumifyProperties;
import io.lumify.core.util.RowKeyHelper;
import org.securegraph.Property;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoPropertyHelper {
    public static VideoFrameInfo getVideoFrameInfoFromProperty(Property property) {
        Object mimeType = property.getMetadata().get(RawLumifyProperties.META_DATA_MIME_TYPE);
        if (mimeType == null || !mimeType.equals("text/plain")) {
            return null;
        }
        return getVideoFrameInfo(property.getKey());
    }

    public static VideoFrameInfo getVideoFrameInfo(String propertyKey) {
        Pattern pattern = Pattern.compile("^(.*)" + RowKeyHelper.MINOR_FIELD_SEPARATOR + MediaLumifyProperties.VIDEO_FRAME.getKey() + RowKeyHelper.MINOR_FIELD_SEPARATOR + "([0-9]+)");
        Matcher m = pattern.matcher(propertyKey);
        if (m.find()) {
            VideoFrameInfo videoFrameInfo = new VideoFrameInfo(Long.parseLong(m.group(2)), m.group(1));
            return videoFrameInfo;
        }
        return null;
    }
}
