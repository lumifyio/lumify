package com.altamiracorp.lumify.web.routes.artifact;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.util.List;
import java.util.Map;

public class ArtifactImport extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ArtifactImport.class);

    private static final String PARAMS_FILENAME = "filename";
    private static final String UNKNOWN_FILENAME = "unknown_filename";

    private final FileImporter fileImporter;

    @Inject
    public ArtifactImport(
            final FileImporter fileImporter,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.fileImporter = fileImporter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (ServletFileUpload.isMultipartContent(request)) {
            final List<Part> files = Lists.newArrayList(request.getParts());

            if (files.size() != 1) {
                throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
            }

            final Part file = files.get(0);
            final String fileName = getFilename(file);

            LOGGER.debug("Processing uploaded file: %s", fileName);
            fileImporter.writeFile(file.getInputStream(), fileName);
        } else {
            LOGGER.warn("Could not process request without multipart content");
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
