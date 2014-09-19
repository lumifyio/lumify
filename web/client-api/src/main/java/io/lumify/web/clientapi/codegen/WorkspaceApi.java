package io.lumify.web.clientapi.codegen;

import com.sun.jersey.multipart.FormDataMultiPart;
import io.lumify.web.clientapi.codegen.model.Workspace;

import java.util.HashMap;
import java.util.Map;

public class WorkspaceApi {
    String basePath = "http://localhost:8889";
    ApiInvoker apiInvoker = ApiInvoker.getInstance();

    public ApiInvoker getInvoker() {
        return apiInvoker;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public Workspace getWorkspaceById(String workspaceId) throws ApiException {
        Object postBody = null;
        // verify required params are set
        if (workspaceId == null) {
            throw new ApiException(400, "missing required params");
        }
        // create path and map variables
        String path = "/workspace".replaceAll("\\{format\\}", "json");

        // query params
        Map<String, String> queryParams = new HashMap<String, String>();
        Map<String, String> headerParams = new HashMap<String, String>();
        Map<String, String> formParams = new HashMap<String, String>();

        if (!"null".equals(String.valueOf(workspaceId)))
            queryParams.put("workspaceId", String.valueOf(workspaceId));
        String[] contentTypes = {
                "application/json"};

        String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

        if (contentType.startsWith("multipart/form-data")) {
            boolean hasFields = false;
            FormDataMultiPart mp = new FormDataMultiPart();
            if (hasFields)
                postBody = mp;
        } else {
        }

        try {
            String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, postBody, headerParams, formParams, contentType);
            if (response != null) {
                return (Workspace) ApiInvoker.deserialize(response, "", Workspace.class);
            } else {
                return null;
            }
        } catch (ApiException ex) {
            if (ex.getCode() == 404) {
                return null;
            } else {
                throw ex;
            }
        }
    }

    public Workspace newWorkspace() throws ApiException {
        Object postBody = null;
        // create path and map variables
        String path = "/workspace/new".replaceAll("\\{format\\}", "json");

        // query params
        Map<String, String> queryParams = new HashMap<String, String>();
        Map<String, String> headerParams = new HashMap<String, String>();
        Map<String, String> formParams = new HashMap<String, String>();

        String[] contentTypes = {
                "application/json"};

        String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

        if (contentType.startsWith("multipart/form-data")) {
            boolean hasFields = false;
            FormDataMultiPart mp = new FormDataMultiPart();
            if (hasFields)
                postBody = mp;
        } else {
        }

        try {
            String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
            if (response != null) {
                return (Workspace) ApiInvoker.deserialize(response, "", Workspace.class);
            } else {
                return null;
            }
        } catch (ApiException ex) {
            if (ex.getCode() == 404) {
                return null;
            } else {
                throw ex;
            }
        }
    }
}

