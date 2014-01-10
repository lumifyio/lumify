package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.ingest.video.VideoPlaybackDetails;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRepository;
import com.altamiracorp.lumify.core.model.artifact.ArtifactRowKey;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtifactRaw extends BaseRequestHandler {
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=([0-9]*)-([0-9]*)");

    private final ArtifactRepository artifactRepository;
    private final GraphRepository graphRepository;

    @Inject
    public ArtifactRaw(final ArtifactRepository repo, final GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
        artifactRepository = repo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        boolean download = getOptionalParameter(request, "download") != null;
        boolean videoPlayback = getOptionalParameter(request, "playback") != null;

        User user = getUser(request);
        String graphVertexId = UrlUtils.urlDecode(getAttributeString(request, "graphVertexId"));

        ArtifactRowKey artifactKey = artifactRepository.findRowKeyByGraphVertexId(graphVertexId, user);
        if (artifactKey == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            chain.next(request, response);
            return;
        }

        Artifact artifact = artifactRepository.findByRowKey(artifactKey.toString(), user.getModelUserContext());
        if (artifact == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            chain.next(request, response);
            return;
        }

        GraphVertex vertex = graphRepository.findVertex(graphVertexId, user);

        String fileName = getFileName(artifact);
        if (videoPlayback) {
            handlePartialPlayback(request, response, artifact, fileName);
        } else {
            String mimeType = getMimeType(artifact);
            response.setContentType(mimeType);
            if (download) {
                response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
            } else {
                response.addHeader("Content-Disposition", "inline; filename=" + fileName);
            }
            InputStream in = artifactRepository.getRaw(artifact, vertex, user);
            try {
                IOUtils.copy(in, response.getOutputStream());
            } finally {
                in.close();
            }
        }

        chain.next(request, response);
    }

    private void handlePartialPlayback(HttpServletRequest request, HttpServletResponse response, Artifact artifact, String fileName) throws IOException {
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
            final String videoFormat = FilenameUtils.getBaseName(videoType);
            response.setContentType(videoType);
            response.addHeader("Content-Disposition", "attachment; filename=" + fileName);

            VideoPlaybackDetails videoDetails = artifactRepository.getVideoPlaybackDetails(artifact.getRowKey().toString(), videoFormat);
            in = videoDetails.getVideoStream();
            totalLength = videoDetails.getVideoFileSize();
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

    private String getFileName(Artifact artifact) {
        return artifact.getMetadata().getFileName();
    }

    private String getMimeType(Artifact artifact) {
        String mimeType = artifact.getMetadata().getMimeType();
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }
}
