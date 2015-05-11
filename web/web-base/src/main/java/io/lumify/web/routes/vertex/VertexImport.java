package io.lumify.web.routes.vertex;

import com.google.common.io.Files;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.ingest.FileImport;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.clientapi.model.ClientApiArtifactImportResponse;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.Visibility;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VertexImport extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(VertexImport.class);

    private static final String PARAMS_FILENAME = "filename";
    private static final String UNKNOWN_FILENAME = "unknown_filename";
    private final Graph graph;

    private final FileImport fileImport;

    @Inject
    public VertexImport(
            final Graph graph,
            final FileImport fileImport,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
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
            List<FileImport.FileAndVisibility> files = getFileAndVisibilities(request, response, chain, tempDir, authorizations, user);
            if (files == null) {
                return;
            }

            Workspace workspace = getWorkspaceRepository().findById(workspaceId, user);

            List<Vertex> vertices = fileImport.importVertices(workspace, files, user, authorizations);

            respondWithClientApiObject(response, toArtifactImportResponse(vertices));
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    private ClientApiArtifactImportResponse toArtifactImportResponse(List<Vertex> vertices) {
        ClientApiArtifactImportResponse response = new ClientApiArtifactImportResponse();
        for (Vertex vertex : vertices) {
            response.getVertexIds().add(vertex.getId());
        }
        return response;
    }

    private List<FileImport.FileAndVisibility> getFileAndVisibilities(HttpServletRequest request, HttpServletResponse response, HandlerChain chain, File tempDir, Authorizations authorizations, User user) throws Exception {
        List<String> invalidVisibilities = new ArrayList<>();
        List<FileImport.FileAndVisibility> files = new ArrayList<>();
        int visibilitySourceIndex = 0;
        int fileIndex = 0;
        for (Part part : request.getParts()) {
            if (part.getName().equals("file")) {
                String fileName = getFilename(part);
                LOGGER.debug("File Name:{}", fileName);
                //file names can have special chars, need to encode them to avoid issues related to saving
                String encodedFileName =  URLEncoder.encode(fileName, "UTF-8");
                LOGGER.debug("Encoded File Name:{}", encodedFileName);
                File outFile = new File(tempDir, encodedFileName);
                copyPartToFile(part, outFile);
                addFileToFilesList(files, fileIndex++, outFile);
            } else if (part.getName().equals("visibilitySource")) {
                String visibilitySource = IOUtils.toString(part.getInputStream(), "UTF8");
                if (!graph.isVisibilityValid(new Visibility(visibilitySource), authorizations)) {
                    invalidVisibilities.add(visibilitySource);
                }
                addVisibilityToFilesList(files, visibilitySourceIndex++, visibilitySource);
            }
        }

        if (invalidVisibilities.size() > 0) {
            LOGGER.warn("%s is not a valid visibility for %s user", invalidVisibilities.toString(), user.getDisplayName());
            respondWithBadRequest(response, "visibilitySource", getString(request, "visibility.invalid"), invalidVisibilities);
            chain.next(request, response);
            return null;
        }

        return files;
    }

    private void addVisibilityToFilesList(List<FileImport.FileAndVisibility> files, int index, String visibilitySource) {
        ensureFilesSize(files, index);
        files.get(index).setVisibilitySource(visibilitySource);
    }

    private void addFileToFilesList(List<FileImport.FileAndVisibility> files, int index, File file) {
        ensureFilesSize(files, index);
        files.get(index).setFile(file);
    }

    private void ensureFilesSize(List<FileImport.FileAndVisibility> files, int index) {
        while (files.size() <= index) {
            files.add(new FileImport.FileAndVisibility());
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
                try {
                    fileName = URLDecoder.decode(name, "utf8").trim();
                } catch (UnsupportedEncodingException ex) {
                    LOGGER.error("Failed to url decode: " + name, ex);
                    fileName = name.trim();
                }
            }
        }

        return fileName;
    }
}
