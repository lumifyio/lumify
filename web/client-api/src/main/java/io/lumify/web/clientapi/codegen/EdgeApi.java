package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.ApiInvoker;

import io.lumify.web.clientapi.model.ClientApiEdgeWithVertexData;
import com.sun.jersey.multipart.FormDataMultiPart;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.util.*;

public class EdgeApi {
  protected String basePath = "http://localhost:8889";
  protected ApiInvoker apiInvoker = ApiInvoker.getInstance();

  public ApiInvoker getInvoker() {
    return apiInvoker;
  }
  
  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }
  
  public String getBasePath() {
    return basePath;
  }

  //error info- code: 404 reason: "Edge not found" model: <none>
  public ClientApiEdgeWithVertexData getByEdgeId (String graphEdgeId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphEdgeId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/edge/properties".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphEdgeId)))
      queryParams.put("graphEdgeId", String.valueOf(graphEdgeId));
    String[] contentTypes = {
      "application/json"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      if(hasFields)
        postBody = mp;
    }
    else {
      }

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiEdgeWithVertexData) ApiInvoker.deserialize(response, "", ClientApiEdgeWithVertexData.class);
      }
      else {
        return null;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return null;
      }
      else {
        throw ex;
      }
    }
  }
  public ClientApiEdgeWithVertexData create (String sourceGraphVertexId, String destGraphVertexId, String predicateLabel, String visibilitySource, String justificationText, String sourceInfo) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(sourceGraphVertexId == null || destGraphVertexId == null || predicateLabel == null || visibilitySource == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/edge/create".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(sourceGraphVertexId)))
      queryParams.put("sourceGraphVertexId", String.valueOf(sourceGraphVertexId));
    if(!"null".equals(String.valueOf(destGraphVertexId)))
      queryParams.put("destGraphVertexId", String.valueOf(destGraphVertexId));
    if(!"null".equals(String.valueOf(predicateLabel)))
      queryParams.put("predicateLabel", String.valueOf(predicateLabel));
    if(!"null".equals(String.valueOf(visibilitySource)))
      queryParams.put("visibilitySource", String.valueOf(visibilitySource));
    if(!"null".equals(String.valueOf(justificationText)))
      queryParams.put("justificationText", String.valueOf(justificationText));
    if(!"null".equals(String.valueOf(sourceInfo)))
      queryParams.put("sourceInfo", String.valueOf(sourceInfo));
    String[] contentTypes = {
      "application/json"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      if(hasFields)
        postBody = mp;
    }
    else {
      }

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiEdgeWithVertexData) ApiInvoker.deserialize(response, "", ClientApiEdgeWithVertexData.class);
      }
      else {
        return null;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return null;
      }
      else {
        throw ex;
      }
    }
  }
  }

