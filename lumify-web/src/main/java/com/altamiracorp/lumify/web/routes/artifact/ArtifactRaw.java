package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.properties.IdentityLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.properties.StreamingLumifyProperty;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.altamiracorp.lumify.core.model.properties.MediaLumifyProperties.*;
import static com.altamiracorp.lumify.core.model.properties.RawLumifyProperties.*;
import static com.google.common.base.Preconditions.checkNotNull;

public class ArtifactRaw extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactRaw.class);
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=([0-9]*)-([0-9]*)");
    private static final Set<String> VALID_VIDEO_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            VIDEO_TYPE_MP4,
            VIDEO_TYPE_WEBM
    )));
    private static final Set<String> VALID_AUDIO_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            AUDIO_TYPE_MP4,
            AUDIO_TYPE_OGG
    )));

    private final Graph graph;

    @Inject
    public ArtifactRaw(
            final Graph graph,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        boolean download = getOptionalParameter(request, "download") != null;
        boolean playback = getOptionalParameter(request, "playback") != null;

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);

        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));

        Vertex artifactVertex = graph.getVertex(graphVertexId, authorizations);
        if (artifactVertex == null) {
            respondWithNotFound(response);
            return;
        }

        String fileName = FILE_NAME.getPropertyValue(artifactVertex);
        if (fileName == null) {
            fileName = LumifyProperties.TITLE.getPropertyValue(artifactVertex);
        }

        if (playback) {
            handlePartialPlayback(request, response, artifactVertex, fileName, user);
        } else {
            String mimeType = getMimeType(artifactVertex);
            response.setContentType(mimeType);
            if (download) {
                response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
            } else {
                response.addHeader("Content-Disposition", "inline; filename=" + fileName);
            }

            StreamingPropertyValue rawValue = RAW.getPropertyValue(artifactVertex);
            if (rawValue == null) {
                LOGGER.warn("Could not find raw on artifact: %s", artifactVertex.getId().toString());
                respondWithNotFound(response);
                return;
            }
            InputStream in = rawValue.getInputStream();
            try {
                IOUtils.copy(in, response.getOutputStream());
            } finally {
                in.close();
            }
        }

        chain.next(request, response);
    }

    private void handlePartialPlayback(HttpServletRequest request, HttpServletResponse response, Vertex artifactVertex, String fileName, User user) throws IOException {
        String type = getRequiredParameter(request, "type");

        InputStream in;
        Long totalLength = null;
        long partialStart = 0;
        Long partialEnd = null;
        String range = request.getHeader("Range");

        if (range != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

            Matcher m = RANGE_PATTERN.matcher(range);
            if (m.matches()) {
                partialStart = Long.parseLong(m.group(1));
                if (m.group(2).length() > 0) {
                    partialEnd = Long.parseLong(m.group(2));
                }
            }
        }

        if (VALID_VIDEO_TYPES.contains(type) || VALID_AUDIO_TYPES.contains(type)) {
            response.setCharacterEncoding(null);
            response.setContentType(type);
            response.addHeader("Content-Disposition", "attachment; filename=" + fileName);

            StreamingLumifyProperty mediaProperty;
            IdentityLumifyProperty<Long> mediaSizeProperty = null;

            if (VALID_VIDEO_TYPES.contains(type)) {
                mediaProperty = getVideoProperty(type);
                mediaSizeProperty = getVideoSizeProperty(type);
            } else {
                mediaProperty = getAudioProperty(type);
                mediaSizeProperty = getAudioSizeProperty(type);
            }

            StreamingPropertyValue videoPropertyValue = mediaProperty.getPropertyValue(artifactVertex);
            checkNotNull(videoPropertyValue, String.format("Could not find video property %s on artifact %s", mediaProperty.getKey(),
                    artifactVertex.getId()));
            in = videoPropertyValue.getInputStream();

            totalLength = mediaSizeProperty.getPropertyValue(artifactVertex);
            checkNotNull(totalLength, String.format("Could not find total video size %s on vertex %s", mediaSizeProperty.getKey(),
                    artifactVertex.getId()));
        } else {
            throw new RuntimeException("Invalid video type: " + type);
        }

        if (partialEnd == null) {
            partialEnd = totalLength;
        }

        // Ensure that the last byte position is less than the instance-length
        partialEnd = Math.min(partialEnd, totalLength - 1);
        long partialLength = totalLength;

        if (range != null) {
            partialLength = partialEnd - partialStart + 1;
            response.addHeader("Content-Range", "bytes " + partialStart + "-" + partialEnd + "/" + totalLength);
            if (partialStart > 0) {
                in.skip(partialStart);
            }
        }

        response.addHeader("Content-Length", "" + partialLength);

        OutputStream out = response.getOutputStream();
        copy(in, out, partialLength);

        response.flushBuffer();
    }

    private void copy(InputStream in, OutputStream out, Long length) throws IOException {
        byte[] buffer = new byte[1024];
        int read = 0;
        while (length > 0 && (read = in.read(buffer, 0, (int) Math.min(length, buffer.length))) > 0) {
            out.write(buffer, 0, read);
            length -= read;
        }
    }

    private String getMimeType(Vertex artifactVertex) {
        String mimeType = MIME_TYPE.getPropertyValue(artifactVertex);
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }
}
