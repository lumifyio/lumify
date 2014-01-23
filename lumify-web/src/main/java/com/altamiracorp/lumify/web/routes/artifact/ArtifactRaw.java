package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtifactRaw extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactRaw.class);
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=([0-9]*)-([0-9]*)");

    private final Graph graph;

    @Inject
    public ArtifactRaw(final Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        boolean download = getOptionalParameter(request, "download") != null;
        boolean videoPlayback = getOptionalParameter(request, "playback") != null;

        User user = getUser(request);
        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));

        Vertex artifactVertex = graph.getVertex(graphVertexId, user.getAuthorizations());
        if (artifactVertex == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            chain.next(request, response);
            return;
        }

        String fileName = (String) artifactVertex.getPropertyValue(PropertyName.FILE_NAME.toString(), 0);
        if (videoPlayback) {
            handlePartialPlayback(request, response, artifactVertex, fileName, user);
        } else {
            String mimeType = getMimeType(artifactVertex);
            response.setContentType(mimeType);
            if (download) {
                response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
            } else {
                response.addHeader("Content-Disposition", "inline; filename=" + fileName);
            }

            StreamingPropertyValue rawValue = (StreamingPropertyValue) artifactVertex.getPropertyValue(PropertyName.RAW.toString(), 0);
            if (rawValue == null) {
                LOGGER.warn("Could not find raw on artifact: %s", artifactVertex.getId().toString());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                chain.next(request, response);
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
        String videoType = getRequiredParameter(request, "type");

        InputStream in = null;
        long totalLength = 0;
        long partialStart = 0;
        Long partialEnd = null;
        String range = request.getHeader("Range");

        if (range != null) {
            Matcher m = RANGE_PATTERN.matcher(range);
            if (m.matches()) {
                partialStart = Long.parseLong(m.group(1));
                if (m.group(2).length() > 0) {
                    partialEnd = Long.parseLong(m.group(2));
                }
                if (partialEnd == null) {
                    partialEnd = partialStart + 100000 - 1;
                }
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            }
        }

        if (videoType.equals("video/mp4") || videoType.equals("video/webm")) {
            response.setContentType(videoType);
            response.addHeader("Content-Disposition", "attachment; filename=" + fileName);

            String videoPropertyName = PropertyName.VIDEO.toString() + "-" + videoType;
            String videoSizePropertyName = PropertyName.VIDEO_SIZE.toString() + "-" + videoType;

            StreamingPropertyValue videoPropertyValue = (StreamingPropertyValue) artifactVertex.getPropertyValue(videoPropertyName, 0);
            if (videoPropertyValue == null) {
                throw new RuntimeException("Could not find video property " + videoPropertyName + " on artifact " + artifactVertex.getId());
            }

            in = videoPropertyValue.getInputStream();
            totalLength = (Long) artifactVertex.getPropertyValue(videoSizePropertyName, 0);
        } else {
            throw new RuntimeException("Invalid video type: " + videoType);
        }

        if (partialEnd == null) {
            partialEnd = totalLength;
        }

        // Ensure that the last byte position is less than the instance-length
        partialEnd = Math.min(partialEnd, totalLength - 1);

        long partialLength = partialEnd - partialStart + 1;
        response.addHeader("Content-Length", "" + partialLength);
        response.addHeader("Content-Range", "bytes " + partialStart + "-" + partialEnd + "/" + totalLength);
        if (partialStart > 0) {
            in.skip(partialStart);
        }

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
        String mimeType = (String) artifactVertex.getPropertyValue(PropertyName.MIME_TYPE.toString(), 0);
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }
}
