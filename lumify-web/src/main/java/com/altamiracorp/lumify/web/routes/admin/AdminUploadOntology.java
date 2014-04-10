package com.altamiracorp.lumify.web.routes.admin;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.util.FilterIterable;
import com.google.common.io.Files;
import com.google.inject.Inject;
import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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

import static com.altamiracorp.securegraph.util.IterableUtils.toList;

public class AdminUploadOntology extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AdminUploadOntology.class);
    private final OntologyRepository ontologyRepository;

    @Inject
    public AdminUploadOntology(
            final OntologyRepository ontologyRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String documentIRIString = getRequiredParameter(request, "documentIRI");

        List<Part> files = toList(getFiles(request));
        if (files.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }
        Part file = files.get(0);

        File tempFile = File.createTempFile("ontologyUpload", ".bin");
        writeToTempFile(file, tempFile);

        IRI documentIRI = IRI.create(documentIRIString);

        User user = getUser(request);
        writePackage(tempFile, documentIRI, user);

        tempFile.delete();

        respondWithPlaintext(response, "OK");
    }

    private Iterable<Part> getFiles(HttpServletRequest request) throws IOException, ServletException {
        return new FilterIterable<Part>(request.getParts()) {
            @Override
            protected boolean isIncluded(Part part) {
                return part.getName().equals("file");
            }
        };
    }

    private void writePackage(File file, IRI documentIRI, User user) throws Exception {
        ZipFile zipped = new ZipFile(file);
        if (zipped.isValidZipFile()) {
            File tempDir = Files.createTempDir();
            try {
                LOGGER.info("Extracting: %s to %s", file.getAbsoluteFile(), tempDir.getAbsolutePath());
                zipped.extractAll(tempDir.getAbsolutePath());

                File owlFile = findOwlFile(tempDir);
                ontologyRepository.importFile(owlFile, documentIRI);
            } finally {
                FileUtils.deleteDirectory(tempDir);
            }
        } else {
            ontologyRepository.importFile(file, documentIRI);
        }
    }

    private File findOwlFile(File dir) {
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                File found = findOwlFile(child);
                if (found != null) {
                    return found;
                }
            } else if (child.getName().toLowerCase().endsWith(".owl")) {
                return child;
            }
        }
        return null;
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
