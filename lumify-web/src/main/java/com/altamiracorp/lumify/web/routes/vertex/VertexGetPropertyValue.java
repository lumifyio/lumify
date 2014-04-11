package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertexGetPropertyValue extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexGetPropertyValue.class);
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=([0-9]*)-([0-9]*)");

    private Graph graph;

    @Inject
    public VertexGetPropertyValue(
            Graph graph,
            UserRepository userRepository,
            Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        PlaybackOptions playbackOptions = new PlaybackOptions();
        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));
        String propertyName = UrlUtils.urlDecode(getAttributeString(request, "propertyName"));
        String propertyKey = UrlUtils.urlDecode(getAttributeString(request, "propertyKey"));
        playbackOptions.range = getOptionalParameter(request, "Range");
        playbackOptions.download = getOptionalParameter(request, "download") != null;
        playbackOptions.playback = getOptionalParameter(request, "playback") != null;

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        handle(response, graphVertexId, propertyName, propertyKey, playbackOptions, authorizations);
    }

    public class PlaybackOptions {
        public String range;
        public boolean download;
        public boolean playback;
    }

    public void handle(HttpServletResponse response, String graphVertexId, String propertyName, String propertyKey, PlaybackOptions playbackOptions, Authorizations authorizations) throws IOException {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            respondWithNotFound(response, String.format("vertex %s not found", graphVertexId));
            return;
        }

        Property property = vertex.getProperty(propertyKey, propertyName);
        if (property == null) {
            respondWithNotFound(response, String.format("property %s:%s not found on vertex %s", propertyKey, propertyName, vertex.getId()));
            return;
        }

        handle(response, vertex, property, playbackOptions);
    }

    private void handle(HttpServletResponse response, Vertex vertex, Property property, PlaybackOptions playbackOptions) throws IOException {
        String fileName = getFileName(vertex);

        String mimeType = getMimeType(property);
        if (mimeType != null) {
            response.setContentType(mimeType);
        }

        setFileNameHeaders(response, fileName, playbackOptions);

        long totalLength;
        InputStream in;
        if (property.getValue() instanceof StreamingPropertyValue) {
            StreamingPropertyValue streamingPropertyValue = (StreamingPropertyValue) property.getValue();
            in = streamingPropertyValue.getInputStream();
            totalLength = streamingPropertyValue.getLength();
        } else {
            byte[] value = property.getValue().toString().getBytes();
            in = new ByteArrayInputStream(value);
            totalLength = value.length;
        }

        try {
            if (playbackOptions.playback) {
                handlePartialPlayback(response, in, totalLength, playbackOptions);
            } else {
                handleFullPlayback(response, in);
            }
        } finally {
            in.close();
        }
    }

    private void setFileNameHeaders(HttpServletResponse response, String fileName, PlaybackOptions playbackOptions) {
        if (playbackOptions.download) {
            response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
        } else {
            response.addHeader("Content-Disposition", "inline; filename=" + fileName);
        }
    }

    private String getFileName(Vertex vertex) {
        String fileName = RawLumifyProperties.FILE_NAME.getPropertyValue(vertex);
        if (fileName == null) {
            fileName = LumifyProperties.TITLE.getPropertyValue(vertex);
        }
        return fileName;
    }

    private void handleFullPlayback(HttpServletResponse response, InputStream in) throws IOException {
        IOUtils.copy(in, response.getOutputStream());
    }

    private void handlePartialPlayback(HttpServletResponse response, InputStream in, long totalLength, PlaybackOptions playbackOptions) throws IOException {
        long partialStart = 0;
        Long partialEnd = null;

        if (playbackOptions.range != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

            Matcher m = RANGE_PATTERN.matcher(playbackOptions.range);
            if (m.matches()) {
                partialStart = Long.parseLong(m.group(1));
                if (m.group(2).length() > 0) {
                    partialEnd = Long.parseLong(m.group(2));
                }
            }
        }

        if (partialEnd == null) {
            partialEnd = totalLength;
        }

        // Ensure that the last byte position is less than the instance-length
        partialEnd = Math.min(partialEnd, totalLength - 1);
        long partialLength = totalLength;

        if (playbackOptions.range != null) {
            partialLength = partialEnd - partialStart + 1;
            response.addHeader("Content-Range", "bytes " + partialStart + "-" + partialEnd + "/" + totalLength);
        }

        response.addHeader("Content-Length", "" + partialLength);

        OutputStream out = response.getOutputStream();
        IOUtils.copyLarge(in, out, partialStart, partialLength);

        response.flushBuffer();
    }

    private String getMimeType(Property property) {
        String mimeType = (String) property.getMetadata().get(RawLumifyProperties.METADATA_MIME_TYPE);
        if (mimeType != null) {
            return mimeType;
        }
        return null;
    }
}
