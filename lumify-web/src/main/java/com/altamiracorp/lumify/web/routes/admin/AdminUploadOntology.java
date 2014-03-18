package com.altamiracorp.lumify.web.routes.admin;

import com.altamiracorp.lumify.core.cmdline.OwlImport;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.common.io.Files;
import com.google.inject.Inject;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AdminUploadOntology extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AdminUploadOntology.class);
    private final OwlImport owlImport;

    @Inject
    public AdminUploadOntology(
            final OwlImport owlImport,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.owlImport = owlImport;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        List<Part> files = new ArrayList<Part>(request.getParts());
        if (files.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }
        Part file = files.get(0);

        File tempFile = File.createTempFile("ontologyUpload", ".bin");
        writeToTempFile(file, tempFile);

        // TODO get document IRI from user
        IRI documentIRI = IRI.create(tempFile.toURI().toString());

        User user = getUser(request);
        writePackage(tempFile, documentIRI, user);

        tempFile.delete();

        respondWithPlaintext(response, "OK");
    }

    private void writePackage(File file, IRI documentIRI, User user) throws ZipException, OWLOntologyCreationException, IOException {
        ZipFile zipped = new ZipFile(file);
        if (zipped.isValidZipFile()) {
            File tempDir = Files.createTempDir();
            try {
                LOGGER.info("Extracting: %s to %s", file.getAbsoluteFile(), tempDir.getAbsolutePath());
                zipped.extractAll(tempDir.getAbsolutePath());

                File owlFile = findOwlFile(tempDir);
                owlImport.importFile(owlFile, documentIRI, user);
            } finally {
                FileUtils.deleteDirectory(tempDir);
            }
        } else {
            owlImport.importFile(file, documentIRI, user);
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
