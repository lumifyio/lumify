package io.lumify.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.ProxyUser;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.App;
import io.lumify.miniweb.Handler;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.clientapi.model.ClientApiObject;
import io.lumify.web.clientapi.model.Privilege;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.securegraph.Authorizations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Represents the base behavior that a {@link Handler} must support
 * and provides common methods for handler usage
 */
public abstract class BaseRequestHandler implements Handler {
    public static final String LUMIFY_WORKSPACE_ID_HEADER_NAME = "Lumify-Workspace-Id";
    public static final String LUMIFY_TIME_ZONE_HEADER_NAME = "Lumify-TimeZone";
    public static final String TIME_ZONE_ATTRIBUTE_NAME = "timeZone";
    public static final String TIME_ZONE_PARAMETER_NAME = "timeZone";
    public static final String LOCALE_LANGUAGE_PARAMETER = "localeLanguage";
    public static final String LOCALE_COUNTRY_PARAMETER = "localeCountry";
    public static final String LOCALE_VARIANT_PARAMETER = "localeVariant";
    protected static final int EXPIRES_1_HOUR = 60 * 60;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final Configuration configuration;
    private final ObjectMapper objectMapper = ObjectMapperFactory.getInstance();

    protected BaseRequestHandler(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.configuration = configuration;
    }

    @Override
    public abstract void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception;

    protected String getBaseUrl(HttpServletRequest request) {
        String configuredBaseUrl = configuration.get(Configuration.BASE_URL, null);
        if (configuredBaseUrl != null && configuredBaseUrl.trim().length() > 0) {
            return configuredBaseUrl;
        }

        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(serverName);
        if (!(scheme.equals("http") && port == 80 || scheme.equals("https") && port == 443)) {
            sb.append(":").append(port);
        }
        sb.append(contextPath);
        return sb.toString();
    }

    protected Locale getLocale(HttpServletRequest request) {
        String language = getOptionalParameter(request, LOCALE_LANGUAGE_PARAMETER);
        String country = getOptionalParameter(request, LOCALE_COUNTRY_PARAMETER);
        String variant = getOptionalParameter(request, LOCALE_VARIANT_PARAMETER);

        if (language != null) {
            return WebApp.getLocal(language, country, variant);
        }
        return request.getLocale();
    }

    protected ResourceBundle getBundle(HttpServletRequest request) {
        WebApp webApp = getWebApp(request);
        Locale locale = getLocale(request);
        return webApp.getBundle(locale);
    }

    protected String getString(HttpServletRequest request, String key) {
        ResourceBundle resourceBundle = getBundle(request);
        return resourceBundle.getString(key);
    }

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

    protected String[] getOptionalParameterArray(HttpServletRequest request, String parameterName) {
        Preconditions.checkNotNull(request, "The provided request was invalid");

        return request.getParameterValues(parameterName);
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
            paramValues[i] = paramValues[i];
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

        return paramValue;
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

    protected String getTimeZone(final HttpServletRequest request) {
        String timeZone = (String) request.getAttribute(TIME_ZONE_ATTRIBUTE_NAME);
        if (timeZone == null || timeZone.trim().length() == 0) {
            timeZone = request.getHeader(LUMIFY_TIME_ZONE_HEADER_NAME);
            if (timeZone == null || timeZone.trim().length() == 0) {
                timeZone = getOptionalParameter(request, TIME_ZONE_PARAMETER_NAME);
                if (timeZone == null || timeZone.trim().length() == 0) {
                    timeZone = configuration.get(Configuration.DEFAULT_TIME_ZONE, TimeZone.getDefault().getDisplayName());
                }
            }
        }
        return timeZone;
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

    public static void setMaxAge(final HttpServletResponse response, int numberOfSeconds) {
        response.setHeader("Cache-Control", "max-age=" + numberOfSeconds);
    }

    public static String generateETag(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(data);
            return Hex.encodeHexString(md5);
        } catch (NoSuchAlgorithmException e) {
            throw new LumifyException("Could not find MD5", e);
        }
    }

    public static void addETagHeader(final HttpServletResponse response, String eTag) {
        response.setHeader("ETag", "\"" + eTag + "\"");
    }

    public boolean testEtagHeaders(HttpServletRequest request, HttpServletResponse response, String eTag) throws IOException {
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null) {
            if (ifNoneMatch.startsWith("\"") && ifNoneMatch.length() > 2) {
                ifNoneMatch = ifNoneMatch.substring(1, ifNoneMatch.length() - 1);
            }
            if (eTag.equalsIgnoreCase(ifNoneMatch)) {
                addETagHeader(response, eTag);
                respondWithNotModified(response);
                return true;
            }
        }

        return false;
    }

    protected void respondWithNotFound(final HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    protected void respondWithNotModified(final HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
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

    protected void respondWithSuccessJson(HttpServletResponse response) {
        JSONObject result = new JSONObject();
        result.put("success", true);
        respondWithJson(response, result);
    }

    protected void respondWithClientApiObject(HttpServletResponse response, ClientApiObject obj) throws IOException {
        if (obj == null) {
            respondWithNotFound(response);
            return;
        }
        try {
            String jsonObject = objectMapper.writeValueAsString(obj);
            configureResponse(ResponseTypes.JSON_OBJECT, response, jsonObject);
        } catch (JsonProcessingException e) {
            throw new LumifyException("Could not write json", e);
        }
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

            if (response.getWriter().checkError()) {
                throw new ConnectionClosedException();
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

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
