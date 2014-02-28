package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestDebugFilter implements Filter {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RequestDebugFilter.class);
    public static final String LUMIFY_REQUEST_DEBUG = "lumify.request.debug";

    public static final String HEADER_DELAY = "Lumify-Request-Delay-Millis";
    public static final String HEADER_ERROR = "Lumify-Request-Error";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if ("true".equals(System.getProperty(LUMIFY_REQUEST_DEBUG))) {
            LOGGER.warn("Request debugging is enabled");
            if (processDebugCommands(request, response)) {
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean processDebugCommands(ServletRequest request, ServletResponse response) throws IOException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String delay = httpRequest.getHeader(HEADER_DELAY);
            String error = httpRequest.getHeader(HEADER_ERROR);

            if (delay != null) {
                try {
                    LOGGER.warn("Lumify Debug Header Found {}. Delaying for {}", HEADER_DELAY, delay);
                    Thread.sleep(Integer.parseInt(delay));
                } catch (InterruptedException e) { }
            }

            if (error != null) {
                LOGGER.warn("Lumify Debug Header Found {}. Sending error instead: {}", HEADER_DELAY, error);
                Integer code = Integer.parseInt(error);
                ((HttpServletResponse) response).sendError(code);
                return true;
            }
        }

        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
