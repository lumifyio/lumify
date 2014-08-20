package io.lumify.web;

import io.lumify.miniweb.handlers.CSRFHandler;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class LumifyCsrfHandler extends CSRFHandler {
    public static final String PARAMETER_NAME = "csrfToken";
    public static final String HEADER_NAME = "Lumify-CSRF-Token";

    public LumifyCsrfHandler() {
        super(PARAMETER_NAME, HEADER_NAME);
    }
}
