package io.lumify.core.ingest.video;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.MediaLumifyProperties;
import io.lumify.core.util.RowKeyHelper;
import org.securegraph.Property;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoPropertyHelper {
    private static final Pattern START_TIME_AND_END_TIME_PATTERN = Pattern.compile("^(.*)" + RowKeyHelper.MINOR_FIELD_SEPARATOR + MediaLumifyProperties.VIDEO_FRAME.getPropertyName() + RowKeyHelper.MINOR_FIELD_SEPARATOR + "([0-9]+)" + RowKeyHelper.MINOR_FIELD_SEPARATOR + "([0-9]+)");
    private static final Pattern START_TIME_ONLY_PATTERN = Pattern.compile("^(.*)" + RowKeyHelper.MINOR_FIELD_SEPARATOR + MediaLumifyProperties.VIDEO_FRAME.getPropertyName() + RowKeyHelper.MINOR_FIELD_SEPARATOR + "([0-9]+)");

    public static VideoFrameInfo getVideoFrameInfoFromProperty(Property property) {
        String mimeType = (String) property.getMetadata().getValue(LumifyProperties.META_DATA_MIME_TYPE);
        if (mimeType == null || !mimeType.equals("text/plain")) {
            return null;
        }
        return getVideoFrameInfo(property.getKey());
    }

    public static VideoFrameInfo getVideoFrameInfo(String propertyKey) {
        Matcher m = START_TIME_AND_END_TIME_PATTERN.matcher(propertyKey);
        if (m.find()) {
            return new VideoFrameInfo(Long.parseLong(m.group(2)), Long.parseLong(m.group(3)), m.group(1));
        }

        m = START_TIME_ONLY_PATTERN.matcher(propertyKey);
        if (m.find()) {
            return new VideoFrameInfo(Long.parseLong(m.group(2)), null, m.group(1));
        }
        return null;
    }
}
