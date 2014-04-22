package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.ingest.FileImport;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Authorizations;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArtifactImport extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactImport.class);

    private static final String PARAMS_FILENAME = "filename";
    private static final String UNKNOWN_FILENAME = "unknown_filename";
    private final Graph graph;

    private final FileImport fileImport;

    @Inject
    public ArtifactImport(
            final Graph graph,
            final FileImport fileImport,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.graph = graph;
        this.fileImport = fileImport;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request)) {
            LOGGER.warn("Could not process request without multi-part content");
            respondWithBadRequest(response, "file", "Could not process request without multi-part content");
            return;
        }

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);
        File tempDir = Files.createTempDir();
        try {
            List<FileAndVisibility> files = getFileAndVisibilities(request, response, chain, tempDir, authorizations, user);
            if (files == null) {
                return;
            }

            List<Vertex> vertices = importVertices(authorizations, workspaceId, files);

            JSONArray vertexIdsJson = getVertexIdsJsonArray(vertices);

            JSONObject json = new JSONObject();
            json.put("vertexIds", vertexIdsJson);
            respondWithJson(response, json);
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    private JSONArray getVertexIdsJsonArray(List<Vertex> vertices) {
        JSONArray vertexIdsJson = new JSONArray();
        for (Vertex v : vertices) {
            vertexIdsJson.put(v.getId().toString());
        }
        return vertexIdsJson;
    }

    private List<Vertex> importVertices(Authorizations authorizations, String workspaceId, List<FileAndVisibility> files) throws Exception {
        List<Vertex> vertices = new ArrayList<Vertex>();
        for (FileAndVisibility file : files) {
            LOGGER.debug("Processing file: %s", file.getFile().getAbsolutePath());
            vertices.add(fileImport.importFile(file.getFile(), true, file.getVisibilitySource(), workspaceId, authorizations));
        }
        return vertices;
    }

    private List<FileAndVisibility> getFileAndVisibilities(HttpServletRequest request, HttpServletResponse response, HandlerChain chain, File tempDir, Authorizations authorizations, User user) throws Exception {
        List<FileAndVisibility> files = new ArrayList<FileAndVisibility>();
        int visibilitySourceIndex = 0;
        int fileIndex = 0;
        for (Part part : request.getParts()) {
            if (part.getName().equals("file")) {
                String fileName = getFilename(part);
                File outFile = new File(tempDir, fileName);
                copyToFile(part, outFile);
                addFileToFilesList(files, fileIndex++, outFile);
            } else if (part.getName().equals("visibilitySource")) {
                String visibilitySource = IOUtils.toString(part.getInputStream(), "UTF8");
                if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
                    LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
                    respondWithBadRequest(response, "visibilitySource", STRINGS.getString("visibility.invalid"), visibilitySource);
                    chain.next(request, response);
                    return null;
                }
                addVisibilityToFilesList(files, visibilitySourceIndex++, visibilitySource);
            }
        }
        return files;
    }

    private void addVisibilityToFilesList(List<FileAndVisibility> files, int index, String visibilitySource) {
        ensureFilesSize(files, index);
        files.get(index).setVisibilitySource(visibilitySource);
    }

    private void addFileToFilesList(List<FileAndVisibility> files, int index, File file) {
        ensureFilesSize(files, index);
        files.get(index).setFile(file);
    }

    private void ensureFilesSize(List<FileAndVisibility> files, int index) {
        while (files.size() <= index) {
            files.add(new FileAndVisibility());
        }
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

    private class FileAndVisibility {
        private File file;
        private String visibilitySource;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getVisibilitySource() {
            return visibilitySource;
        }

        public void setVisibilitySource(String visibilitySource) {
            this.visibilitySource = visibilitySource;
        }
    }
}
