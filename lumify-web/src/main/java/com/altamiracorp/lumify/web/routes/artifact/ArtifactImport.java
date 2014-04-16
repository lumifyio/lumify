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
import com.altamiracorp.securegraph.Visibility;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
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
            LOGGER.warn("Could not process request without multipart content");
            respondWithBadRequest(response, "file", "Could not process request without multipart content");
            return;
        }

        final List<Part> files = Lists.newArrayList(request.getParts());

        if (files.size() != 1) {
            throw new LumifyException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }

        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        Visibility visibility = new Visibility(""); // TODO fill this in with request parameter

        final Part file = files.get(0);
        final String fileName = getFilename(file);
        File tempDir = Files.createTempDir();
        try {
            File f = new File(tempDir, fileName);
            copyToFile(file, f);
            try {
                LOGGER.debug("Processing uploaded file: %s", fileName);
                Vertex vertex = fileImport.importFile(f, true, visibility, authorizations);

                JSONObject json = new JSONObject();
                json.put("vertexId", vertex.getId().toString());
                respondWithJson(response, json);
            } finally {
                f.delete();
            }
        } finally {
            tempDir.delete();
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
}
