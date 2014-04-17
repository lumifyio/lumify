package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.ingest.FileImport;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Vertex;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ArtifactImport extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactImport.class);

    private static final String PARAMS_FILENAME = "filename";
    private static final String UNKNOWN_FILENAME = "unknown_filename";

    private final FileImport fileImport;

    @Inject
    public ArtifactImport(
            final FileImport fileImport,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.fileImport = fileImport;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request)) {
            LOGGER.warn("Could not process request without multi-part content");
            respondWithBadRequest(response, "file", "Could not process request without multi-part content");
            return;
        }

        final List<Part> files = Lists.newArrayList(request.getParts());

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String visibilitySource = ""; // TODO fill this in with request parameter
        String workspaceId = getActiveWorkspaceId(request);

        List<Vertex> vertices;
        File tempDir = copyToTempDirectory(files);
        try {
            LOGGER.debug("Processing upload: %s", tempDir);
            vertices = fileImport.importDirectory(tempDir, true, visibilitySource, workspaceId, authorizations);
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }

        JSONArray vertexIdsJson = new JSONArray();
        for (Vertex v : vertices) {
            vertexIdsJson.put(v.getId().toString());
        }

        JSONObject json = new JSONObject();
        json.put("vertexIds", vertexIdsJson);
        respondWithJson(response, json);
    }

    private File copyToTempDirectory(List<Part> files) throws IOException {
        File tempDir = Files.createTempDir();
        try {
            for (Part filePart : files) {
                String fileName = getFilename(filePart);
                File f = new File(tempDir, fileName);
                copyToFile(filePart, f);
            }
        } catch (Exception ex) {
            FileUtils.deleteDirectory(tempDir);
            throw new LumifyException("Could not copy files to temp directory: " + tempDir.getAbsolutePath(), ex);
        }
        return tempDir;
    }

    private void copyToFile(Part part, File outFile) throws IOException {
        FileOutputStream out = new FileOutputStream(outFile);
        InputStream in = part.getInputStream();
        try {
            IOUtils.copy(in, out);
        } finally {
            out.close();
            in.close();
        }
    }

    private static String getFilename(Part part) {
        String fileName = UNKNOWN_FILENAME;

        final ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);

        final Map params = parser.parse(part.getHeader(FileUploadBase.CONTENT_DISPOSITION), ';');
        if (params.containsKey(PARAMS_FILENAME)) {
            final String name = (String) params.get(PARAMS_FILENAME);
            if (name != null) {
                fileName = name.trim();
            }
        }

        return fileName;
    }
}
