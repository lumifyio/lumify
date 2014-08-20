package io.lumify.web.importExportWorkspaces;

import io.lumify.miniweb.HandlerChain;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.securegraph.model.user.SecureGraphUserRepository;
import io.lumify.securegraph.model.workspace.SecureGraphWorkspaceRepository;
import io.lumify.web.BaseRequestHandler;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.tools.GraphRestore;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Import extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Import.class);
    private final SecureGraphWorkspaceRepository workspaceRepository;
    private final Graph graph;

    @Inject
    public Import(
            UserRepository userRepository,
            Configuration configuration,
            WorkspaceRepository workspaceRepository,
            Graph graph) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = (SecureGraphWorkspaceRepository) workspaceRepository;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (!ServletFileUpload.isMultipartContent(request)) {
            LOGGER.warn("Could not process request without multi-part content");
            respondWithBadRequest(response, "file", "Could not process request without multi-part content");
            return;
        }

        User user = getUser(request);
        Vertex userVertex = ((SecureGraphUserRepository) getUserRepository()).findByIdUserVertex(user.getUserId());
        GraphRestore graphRestore = new GraphRestore();

        for (Part part : request.getParts()) {
            if (part.getName().equals("workspace")) {
                File outFile = File.createTempFile("lumifyWorkspaceImport", "lumifyworkspace");
                copyPartToFile(part, outFile);

                String workspaceId = getWorkspaceId(outFile);

                Authorizations authorizations = getUserRepository().getAuthorizations(user, UserRepository.VISIBILITY_STRING, WorkspaceRepository.VISIBILITY_STRING, workspaceId);

                InputStream in = new FileInputStream(outFile);
                try {
                    graphRestore.restore(graph, in, authorizations);

                    Vertex workspaceVertex = this.workspaceRepository.getVertex(workspaceId, user);
                    this.workspaceRepository.addWorkspaceToUser(workspaceVertex, userVertex, authorizations);
                } finally {
                    graph.flush();
                    in.close();
                }
            }
        }

        respondWithPlaintext(response, "Workspace Imported");
    }

    private String getWorkspaceId(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String workspaceLine = in.readLine();
        Matcher m = Pattern.compile(".*\"id\":\"(.*?)\".*").matcher(workspaceLine);
        if (!m.matches()) {
            throw new LumifyException("Could not find Workspace id in line: " + workspaceLine);
        }
        return m.group(1);
    }
}
