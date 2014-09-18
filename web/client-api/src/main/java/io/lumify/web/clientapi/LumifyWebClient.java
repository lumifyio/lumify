package io.lumify.web.clientapi;

import io.lumify.web.clientapi.model.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

public abstract class LumifyWebClient {
    private final String MULTIPART_BOUNDARY = "sdlfkgjmwerijlsslkdfj";
    private String csrfToken;
    private String currentWorkspaceId;

    public UserMeResponse userMe() {
        JSONObject responseJson = httpGetJson("/user/me");
        UserMeResponse userMeResponse = new UserMeResponse(responseJson);
        csrfToken = userMeResponse.getCsrfToken();
        currentWorkspaceId = userMeResponse.getCurrentWorkspaceId();
        return userMeResponse;
    }

    public WorkspacesResponse workspaces() {
        return new WorkspacesResponse(httpGetJson("/workspaces"));
    }

    public WorkspaceNewResponse workspaceNew() {
        return new WorkspaceNewResponse(httpPostJson("/workspace/new"));
    }

    public WorkspaceDiffResponse workspaceDiff() {
        return new WorkspaceDiffResponse(httpGetJson("/workspace/diff"));
    }

    public WorkspacePublishResponse workspacePublishAll() {
        WorkspaceDiffResponse workspaceDiffResponse = workspaceDiff();
        return workspacePublishAll(workspaceDiffResponse.getDiffs());
    }

    public WorkspacePublishResponse workspacePublishAll(WorkspaceDiffItem[] diffs) {
        List<PublishItem> publishItems = new ArrayList<PublishItem>();
        for (WorkspaceDiffItem diffItem : diffs) {
            if (diffItem instanceof VertexDiffItem) {
                VertexDiffItem vertexDiffItem = (VertexDiffItem) diffItem;
                publishItems.add(new VertexPublishItem(vertexDiffItem.getVertexId()));
            } else if (diffItem instanceof PropertyDiffItem) {
                PropertyDiffItem propertyDiffItem = (PropertyDiffItem) diffItem;
                publishItems.add(new PropertyPublishItem(propertyDiffItem.getElementId(), propertyDiffItem.getPropertyKey(), propertyDiffItem.getPropertyName()));
            } else if (diffItem instanceof EdgeDiffItem) {
                EdgeDiffItem edgeDiffItem = (EdgeDiffItem) diffItem;
                publishItems.add(new EdgePublishItem(edgeDiffItem.getEdgeId(), edgeDiffItem.getSourceVertexId(), edgeDiffItem.getDestVertexId()));
            } else {
                throw new LumifyClientApiException("Unhandled diff item type: " + diffItem.getClass().getName());
            }
        }
        return workspacePublish(publishItems);
    }

    public WorkspacePublishResponse workspacePublish(Iterable<PublishItem> publishItems) {
        WorkspacePublishResponse response;
        try {
            JSONArray json = new JSONArray();
            for (PublishItem publishItem : publishItems) {
                json.put(publishItem.getJson());
            }
            response = new WorkspacePublishResponse(httpPostJson("/workspace/publish", createPostContent("publishData", json.toString())));
        } catch (Exception ex) {
            throw new LumifyClientApiException("Could not publish workspace", ex);
        }
        if (response.getFailures().length > 0) {
            throw new LumifyClientApiPublishException(response);
        }
        return response;
    }

    public ArtifactImportResponse artifactImport(String visibilitySource, String fileName, InputStream file) {
        try {
            Map<String, List<String>> headers = new HashMap<String, List<String>>();
            headers.put("Content-Type", Arrays.asList("multipart/form-data;boundary=" + MULTIPART_BOUNDARY));

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            appendMulipartFormData(buffer, MULTIPART_BOUNDARY, "visibilitySource", null, new ByteArrayInputStream(visibilitySource.getBytes()), false);
            appendMulipartFormData(buffer, MULTIPART_BOUNDARY, "file", fileName, file, true);

            InputStream content = new ByteArrayInputStream(buffer.toByteArray());

            return new ArtifactImportResponse(httpPostJson("/artifact/import", headers, content));
        } catch (Exception ex) {
            throw new LumifyClientApiException("Could not import artifact", ex);
        }
    }

    public WorkspaceVerticesResponse workspaceVertices() {
        return new WorkspaceVerticesResponse(httpGetJsonArray("/workspace/vertices"));
    }

    public WorkspaceUpdateResponse workspaceUpdate(List<WorkspaceUpdateItem> workspaceUpdateItems) {
        try {
            JSONObject json = new JSONObject();
            JSONArray entityUpdates = new JSONArray();
            json.put("entityUpdates", entityUpdates);
            JSONArray entityDeletes = new JSONArray();
            json.put("entityDeletes", entityDeletes);
            JSONArray userUpdates = new JSONArray();
            json.put("userUpdates", userUpdates);
            JSONArray userDeletes = new JSONArray();
            json.put("userDeletes", userDeletes);
            for (WorkspaceUpdateItem workspaceUpdateItem : workspaceUpdateItems) {
                if (workspaceUpdateItem instanceof VertexWorkspaceUpdateItem) {
                    entityUpdates.put(workspaceUpdateItem.getJson());
                } else {
                    throw new LumifyClientApiException("Unhandled workspace update item type: " + workspaceUpdateItem.getClass().getName());
                }
            }
            return new WorkspaceUpdateResponse(httpPostJson("/workspace/update", createPostContent("data", json.toString())));
        } catch (Exception ex) {
            throw new LumifyClientApiException("Could not update workspace", ex);
        }
    }

    public void logInToCurrentWorkspace() {
        UserMeResponse userMeResponse = userMe();

        WorkspacesResponse workspacesResponse = workspaces();

        Workspace currentWorkspace;
        Workspace[] workspaces = workspacesResponse.getWorkspaces();
        if (workspaces.length == 0) {
            WorkspaceNewResponse workspaceNewResponse = workspaceNew();
            currentWorkspace = workspaceNewResponse.getWorkspace();
        } else {
            if (userMeResponse.getCurrentWorkspaceId() == null) {
                currentWorkspace = workspaces[0];
            } else {
                currentWorkspace = null;
                for (Workspace w : workspaces) {
                    if (w.getId().equals(userMeResponse.getCurrentWorkspaceId())) {
                        currentWorkspace = w;
                        break;
                    }
                }
                if (currentWorkspace == null) {
                    currentWorkspace = workspaces[0];
                }
            }
        }

        setCurrentWorkspaceId(currentWorkspace.getId());
    }

    public void logOut() {
        httpPostJson("/logout");
    }

    public String artifactHighlightedText(String vertexId, String propertyKey) {
        try {
            HttpResponse response = httpGet("/artifact/highlightedText?graphVertexId=" + URLEncoder.encode(vertexId, "UTF8") + "&propertyKey=" + URLEncoder.encode(propertyKey, "UTF8"));
            return IOUtils.toString(response.getInputStream());
        } catch (IOException e) {
            throw new LumifyClientApiException("Could not get artifact highlighted text", e);
        }
    }

    public void entityResolveTerm(String vertexId, String propertyKey, int start, int end, String sign, String conceptIri, String visibilitySource) {
        try {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("artifactId", vertexId);
            parameters.put("propertyKey", propertyKey);
            parameters.put("mentionStart", Integer.toString(start));
            parameters.put("mentionEnd", Integer.toString(end));
            parameters.put("sign", sign);
            parameters.put("conceptId", conceptIri);
            parameters.put("visibilitySource", visibilitySource);
            InputStream content = createPostContent(parameters);
            httpPostJson("/entity/resolveTerm", content);
        } catch (Exception ex) {
            throw new LumifyClientApiException("Could not resolve term", ex);
        }
    }

    public GraphVertexSearchResponse graphVertexSearch(String query) {
        try {
            String q = "q=" + URLEncoder.encode(query, "UTF8");
            String filter = "filter=" + URLEncoder.encode("[]", "UTF8");
            JSONObject json = httpGetJson("/graph/vertex/search?" + q + "&" + filter);
            return new GraphVertexSearchResponse(json);
        } catch (Exception ex) {
            throw new LumifyClientApiException("could not search", ex);
        }
    }

    public void adminUploadOntology(InputStream owlFile) {
        try {
            Map<String, List<String>> headers = new HashMap<String, List<String>>();
            headers.put("Content-Type", Arrays.asList("multipart/form-data;boundary=" + MULTIPART_BOUNDARY));

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            appendMulipartFormData(buffer, MULTIPART_BOUNDARY, "file", null, owlFile, true);

            InputStream content = new ByteArrayInputStream(buffer.toByteArray());

            httpPostJson("/admin/uploadOntology", headers, content);
        } catch (Exception ex) {
            throw new LumifyClientApiException("Could not upload ontology", ex);
        }
    }

    private InputStream createPostContent(String... parameters) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < parameters.length; i += 2) {
            map.put(parameters[i], parameters[i + 1]);
        }
        return createPostContent(map);
    }

    private InputStream createPostContent(Map<String, String> parameters) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean first = true;
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            if (!first) {
                buffer.write("&".getBytes());
            }
            buffer.write(parameter.getKey().getBytes());
            buffer.write("=".getBytes());
            buffer.write(URLEncoder.encode(parameter.getValue(), "UTF8").getBytes());
            first = false;
        }
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    private void appendMulipartFormData(OutputStream buffer, String boundary, String fieldName, String fileName, InputStream fieldData, boolean lastPart) throws IOException {
        buffer.write(("--" + boundary + "\r\n").getBytes());
        buffer.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"" + (fileName == null ? "" : ";filename=\"" + fileName + "\"") + "\r\n").getBytes());
        buffer.write("\r\n".getBytes());
        IOUtils.copy(fieldData, buffer);
        buffer.write("\r\n".getBytes());
        buffer.write(("--" + boundary + (lastPart ? "--" : "") + "\r\n").getBytes());
    }

    protected JSONObject httpPostJson(String uri) {
        return httpPostJson(uri, null, null);
    }

    protected JSONObject httpPostJson(String uri, InputStream content) {
        return httpPostJson(uri, null, content);
    }

    protected JSONObject httpPostJson(String uri, Map<String, List<String>> additionalHeaders, InputStream content) {
        try {
            HttpResponse response = httpPost(uri, additionalHeaders, content);
            InputStream in = response.getInputStream();
            String str = IOUtils.toString(in);
            return new JSONObject(str);
        } catch (Exception ex) {
            throw new LumifyClientApiException("httpGetJson failed", ex);
        }
    }

    protected JSONObject httpGetJson(String uri) {
        try {
            HttpResponse response = httpGet(uri);
            InputStream in = response.getInputStream();
            String str = IOUtils.toString(in);
            return new JSONObject(str);
        } catch (Exception ex) {
            throw new LumifyClientApiException("httpGetJson failed", ex);
        }
    }

    protected JSONArray httpGetJsonArray(String uri) {
        try {
            HttpResponse response = httpGet(uri);
            InputStream in = response.getInputStream();
            String str = IOUtils.toString(in);
            return new JSONArray(str);
        } catch (Exception ex) {
            throw new LumifyClientApiException("httpGetJson failed", ex);
        }
    }

    protected abstract HttpResponse httpGet(String uri);

    protected abstract HttpResponse httpPost(String uri, Map<String, List<String>> additionalHeaders, InputStream content);

    protected String getCsrfToken() {
        return csrfToken;
    }

    /**
     * This sets the current workspace id for requests. This does not change the server state of current workspace id.
     */
    public void setCurrentWorkspaceId(String currentWorkspaceId) {
        this.currentWorkspaceId = currentWorkspaceId;
    }

    public String getCurrentWorkspaceId() {
        return currentWorkspaceId;
    }
}
