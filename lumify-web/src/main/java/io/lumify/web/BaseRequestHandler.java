package io.lumify.web;

import com.altamiracorp.miniweb.App;
import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.google.common.base.Preconditions;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.Authorizations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.util.*;

/**
 * Represents the base behavior that a {@link Handler} must support
 * and provides common methods for handler usage
 */
public abstract class BaseRequestHandler implements Handler {

    public static final String LUMIFY_WORKSPACE_ID_HEADER_NAME = "Lumify-Workspace-Id";

    protected static final ResourceBundle STRINGS = ResourceBundle.getBundle("MessageBundle", Locale.getDefault());

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final Configuration configuration;

    protected BaseRequestHandler(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.configuration = configuration;
    }

    @Override
    public abstract void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception;

    /**
     * Attempts to extract the specified parameter from the provided request
     *
     * @param request       The request instance containing the parameter
     * @param parameterName The name of the parameter to extract
     * @return The value of the specified parameter
     * @throws RuntimeException Thrown if the required parameter was not in the request
     */
    protected String getRequiredParameter(final HttpServletRequest request, final String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return getParameter(request, parameterName, false);
    }

    protected String[] getRequiredParameterArray(HttpServletRequest request, String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        String[] value = request.getParameterValues(parameterName);
        if (value == null) {
            throw new LumifyException(String.format("Parameter: '%s' is required in the request", parameterName));
        }
        return value;
    }

    protected long getOptionalParameterLong(final HttpServletRequest request, final String parameterName, long defaultValue) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null) {
            return defaultValue;
        }
        return Long.parseLong(val);
    }

    protected boolean getOptionalParameterBoolean(final HttpServletRequest request, final String parameterName, boolean defaultValue) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val);
    }

    protected double getOptionalParameterDouble(final HttpServletRequest request, final String parameterName, double defaultValue) {
        String val = getOptionalParameter(request, parameterName);
        if (val == null) {
            return defaultValue;
        }
        return Double.parseDouble(val);
    }

    /**
     * Attempts to extract the specified parameter from the provided request and convert it to a long value
     *
     * @param request       The request instance containing the parameter
     * @param parameterName The name of the parameter to extract
     * @return The long value of the specified parameter
     * @throws RuntimeException Thrown if the required parameter was not in the request
     */
    protected long getRequiredParameterAsLong(final HttpServletRequest request, final String parameterName) {
        return Long.parseLong(getRequiredParameter(request, parameterName));
    }


    /**
     * Attempts to extract the specified parameter from the provided request and convert it to a double value
     *
     * @param request       The request instance containing the parameter
     * @param parameterName The name of the parameter to extract
     * @return The double value of the specified parameter
     * @throws RuntimeException Thrown if the required parameter was not in the request
     */
    protected double getRequiredParameterAsDouble(final HttpServletRequest request, final String parameterName) {
        return Double.parseDouble(getRequiredParameter(request, parameterName));
    }


    /**
     * Attempts to extract the specified parameter from the provided request, if available
     *
     * @param request       The request instance containing the parameter
     * @param parameterName The name of the parameter to extract
     * @return The value of the specified parameter if found, null otherwise
     */
    protected String getOptionalParameter(final HttpServletRequest request, final String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return getParameter(request, parameterName, true);
    }

    protected String[] getOptionalParameterAsStringArray(final HttpServletRequest request, final String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return getParameterValues(request, parameterName, true);
    }

    protected String[] getParameterValues(final HttpServletRequest request, final String parameterName, final boolean optional) {
        final String[] paramValues = request.getParameterValues(parameterName);

        if (paramValues == null) {
            if (!optional) {
                throw new RuntimeException(String.format("Parameter: '%s' is required in the request", parameterName));
            }
            return null;
        }

        for (int i = 0; i < paramValues.length; i++) {
            paramValues[i] = UrlUtils.urlDecode(paramValues[i]);
        }

        return paramValues;
    }

    private String getParameter(final HttpServletRequest request, final String parameterName, final boolean optional) {
        final String paramValue = request.getParameter(parameterName);

        if (paramValue == null) {
            if (!optional) {
                throw new LumifyException(String.format("Parameter: '%s' is required in the request", parameterName));
            }

            return null;
        }

        return UrlUtils.urlDecode(paramValue);
    }

    protected String getAttributeString(final HttpServletRequest request, final String name) {
        String attr = (String) request.getAttribute(name);
        if (attr != null) {
            return attr;
        }
        return getRequiredParameter(request, name);
    }

    protected String getActiveWorkspaceId(final HttpServletRequest request) {
        String workspaceId = getWorkspaceIdOrDefault(request);
        if (workspaceId == null || workspaceId.trim().length() == 0) {
            throw new LumifyException(LUMIFY_WORKSPACE_ID_HEADER_NAME + " is a required header.");
        }
        return workspaceId;
    }

    protected String getWorkspaceIdOrDefault(final HttpServletRequest request) {
        String workspaceId = (String) request.getAttribute("workspaceId");
        if (workspaceId == null || workspaceId.trim().length() == 0) {
            workspaceId = request.getHeader(LUMIFY_WORKSPACE_ID_HEADER_NAME);
            if (workspaceId == null || workspaceId.trim().length() == 0) {
                workspaceId = getOptionalParameter(request, "workspaceId");
                if (workspaceId == null || workspaceId.trim().length() == 0) {
                    return null;
                }
            }
        }
        return workspaceId;
    }

    protected Authorizations getAuthorizations(final HttpServletRequest request, final User user) {
        String workspaceId = getWorkspaceIdOrDefault(request);
        if (workspaceId != null) {
            if (!this.workspaceRepository.hasReadPermissions(workspaceId, user)) {
                throw new LumifyAccessDeniedException("You do not have access to workspace: " + workspaceId, user, workspaceId);
            }
            return getUserRepository().getAuthorizations(user, workspaceId);
        }

        return getUserRepository().getAuthorizations(user);
    }

    protected Set<Privilege> getPrivileges(User user) {
        return getUserRepository().getPrivileges(user);
    }

    protected void respondWithNotFound(final HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    protected void respondWithNotFound(final HttpServletResponse response, String message) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
    }

    protected void respondWithAccessDenied(final HttpServletResponse response, String message) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, message);
    }

    /**
     * Send a Bad Request response with JSON object mapping field error messages
     *
     * @param response
     * @param parameterName
     * @param errorMessage
     * @param invalidValue
     */
    protected void respondWithBadRequest(final HttpServletResponse response, final String parameterName, final String errorMessage, final String invalidValue) throws IOException {
        List<String> values = null;

        if (invalidValue != null) {
            values = new ArrayList<String>();
            values.add(invalidValue);
        }
        respondWithBadRequest(response, parameterName, errorMessage, values);
    }


    /**
     * Send a Bad Request response with JSON object mapping field error messages
     *
     * @param response
     * @param parameterName
     * @param errorMessage
     * @param invalidValues
     */
    protected void respondWithBadRequest(final HttpServletResponse response, final String parameterName, final String errorMessage, final List<String> invalidValues) throws IOException {
        JSONObject error = new JSONObject();
        error.put(parameterName, errorMessage);
        if (invalidValues != null) {
            JSONArray values = new JSONArray();
            for (String v : invalidValues) {
                values.put(v);
            }
            error.put("invalidValues", values);
        }
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, error.toString());
    }

    /**
     * Send a Bad Request response with JSON object mapping field error messages
     *
     * @param response
     * @param parameterName
     * @param errorMessage
     */
    protected void respondWithBadRequest(final HttpServletResponse response, final String parameterName, final String errorMessage) throws IOException {
        respondWithBadRequest(response, parameterName, errorMessage, new ArrayList<String>());
    }

    /**
     * Configures the content type for the provided response to contain {@link JSONObject} data
     *
     * @param response   The response instance to modify
     * @param jsonObject The JSON data to include in the response
     */
    protected void respondWithJson(final HttpServletResponse response, final JSONObject jsonObject) {
        configureResponse(ResponseTypes.JSON_OBJECT, response, jsonObject);
    }


    /**
     * Configures the content type for the provided response to contain {@link JSONArray} data
     *
     * @param response  The response instance to modify
     * @param jsonArray The JSON data to include in the response
     */
    protected void respondWithJson(final HttpServletResponse response, final JSONArray jsonArray) {
        configureResponse(ResponseTypes.JSON_ARRAY, response, jsonArray);
    }

    protected void respondWithPlaintext(final HttpServletResponse response, final String plaintext) {
        configureResponse(ResponseTypes.PLAINTEXT, response, plaintext);
    }

    protected void respondWithHtml(final HttpServletResponse response, final String html) {
        configureResponse(ResponseTypes.HTML, response, html);
    }

    protected User getUser(HttpServletRequest request) {
        return new ProxyUser(CurrentUser.get(request), this.userRepository);
    }

    private void configureResponse(final ResponseTypes type, final HttpServletResponse response, final Object responseData) {
        Preconditions.checkNotNull(response, "The provided response was invalid");
        Preconditions.checkNotNull(responseData, "The provided data was invalid");

        try {
            switch (type) {
                case JSON_OBJECT:
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(responseData.toString());
                    break;
                case JSON_ARRAY:
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(responseData.toString());
                    break;
                case PLAINTEXT:
                    response.setContentType("text/plain");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(responseData.toString());
                    break;
                case HTML:
                    response.setContentType("text/html");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(responseData.toString());
                    break;
                default:
                    throw new RuntimeException("Unsupported response type encountered");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while writing response", e);
        }
    }

    protected void copyPartToOutputStream(Part part, OutputStream out) throws IOException {
        InputStream in = part.getInputStream();
        try {
            IOUtils.copy(in, out);
        } finally {
            out.close();
            in.close();
        }
    }

    protected void copyPartToFile(Part part, File outFile) throws IOException {
        FileOutputStream out = new FileOutputStream(outFile);
        copyPartToOutputStream(part, out);
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public WorkspaceRepository getWorkspaceRepository() {
        return workspaceRepository;
    }

    public WebApp getWebApp(HttpServletRequest request) {
        return (WebApp) App.getApp(request);
    }
}
