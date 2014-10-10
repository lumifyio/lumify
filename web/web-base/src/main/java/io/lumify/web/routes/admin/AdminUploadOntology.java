package io.lumify.web.routes.admin;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.securegraph.Authorizations;
import org.securegraph.util.FilterIterable;
import org.semanticweb.owlapi.model.IRI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.securegraph.util.IterableUtils.toList;

public class AdminUploadOntology extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AdminUploadOntology.class);
    private final OntologyRepository ontologyRepository;

    @Inject
    public AdminUploadOntology(
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String documentIRIString = getOptionalParameter(request, "documentIRI");

        List<Part> files = toList(getFiles(request));
        if (files.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }
        Part file = files.get(0);

        File tempFile = File.createTempFile("ontologyUpload", ".bin");
        writeToTempFile(file, tempFile);

        if (documentIRIString == null || documentIRIString.length() == 0) {
            documentIRIString = ontologyRepository.guessDocumentIRIFromPackage(tempFile);
        }

        IRI documentIRI = IRI.create(documentIRIString);

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        LOGGER.info("adding ontology: %s", documentIRI.toString());
        ontologyRepository.writePackage(tempFile, documentIRI, authorizations);
        ontologyRepository.clearCache();

        tempFile.delete();

        respondWithSuccessJson(response);
    }

    private Iterable<Part> getFiles(HttpServletRequest request) throws IOException, ServletException {
        return new FilterIterable<Part>(request.getParts()) {
            @Override
            protected boolean isIncluded(Part part) {
                return part.getName().equals("file");
            }
        };
    }

    private void writeToTempFile(Part file, File tempFile) throws IOException {
        InputStream in = file.getInputStream();
        try {
            FileOutputStream out = new FileOutputStream(tempFile);
            try {
                IOUtils.copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
