package com.altamiracorp.lumify.web.routes.admin;

import com.altamiracorp.lumify.core.cmdline.OwlImport;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.common.io.Files;
import com.google.inject.Inject;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AdminUploadOntology extends BaseRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUploadOntology.class.getName());
    private final OwlImport owlImport;

    @Inject
    public AdminUploadOntology(OwlImport owlImport) {
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

        User user = getUser(request);
        writePackage(tempFile, user);

        tempFile.delete();

        respondWithPlaintext(response, "OK");
    }

    private void writePackage(File file, User user) throws ZipException, IOException, SAXException, ParserConfigurationException {
        ZipFile zipped = new ZipFile(file);
        if (zipped.isValidZipFile()) {
            File tempDir = Files.createTempDir();
            try {
                LOGGER.info("Extracting: " + file.getAbsoluteFile() + " to " + tempDir.getAbsolutePath());
                zipped.extractAll(tempDir.getAbsolutePath());

                File owlFile = findOwlFile(tempDir);
                owlImport.importFile(owlFile, user);
            } finally {
                FileUtils.deleteDirectory(tempDir);
            }
        } else {
            owlImport.importFile(file, user);
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
