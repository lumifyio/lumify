package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.ApiInvoker;

import io.lumify.web.clientapi.model.Element;
import io.lumify.web.clientapi.codegen.model.DetectedObjects;
import com.sun.jersey.multipart.FormDataMultiPart;
import io.lumify.web.clientapi.model.TermMentionsResponse;
import io.lumify.web.clientapi.model.Vertex;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.util.*;

public class VertexApi {
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

  //error info- code: 404 reason: "Vertex not found" model: <none>
  public Element getByVertexId (String graphVertexId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/properties".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphVertexId)))
      queryParams.put("graphVertexId", String.valueOf(graphVertexId));
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
        return (Element) ApiInvoker.deserialize(response, "", Element.class);
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
  public Vertex create (String conceptType, String visibilitySource) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(conceptType == null || visibilitySource == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/new".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(conceptType)))
      queryParams.put("conceptType", String.valueOf(conceptType));
    if(!"null".equals(String.valueOf(visibilitySource)))
      queryParams.put("visibilitySource", String.valueOf(visibilitySource));
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
        return (Vertex) ApiInvoker.deserialize(response, "", Vertex.class);
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
  public Element setProperty (String graphVertexId, String propertyKey, String propertyName, String value, String visibilitySource, String justificationText, String sourceInfo, String metadata) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || propertyKey == null || propertyName == null || value == null || visibilitySource == null || justificationText == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/property/set".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphVertexId)))
      queryParams.put("graphVertexId", String.valueOf(graphVertexId));
    if(!"null".equals(String.valueOf(propertyKey)))
      queryParams.put("propertyKey", String.valueOf(propertyKey));
    if(!"null".equals(String.valueOf(propertyName)))
      queryParams.put("propertyName", String.valueOf(propertyName));
    if(!"null".equals(String.valueOf(value)))
      queryParams.put("value", String.valueOf(value));
    if(!"null".equals(String.valueOf(visibilitySource)))
      queryParams.put("visibilitySource", String.valueOf(visibilitySource));
    if(!"null".equals(String.valueOf(justificationText)))
      queryParams.put("justificationText", String.valueOf(justificationText));
    if(!"null".equals(String.valueOf(sourceInfo)))
      queryParams.put("sourceInfo", String.valueOf(sourceInfo));
    if(!"null".equals(String.valueOf(metadata)))
      queryParams.put("metadata", String.valueOf(metadata));
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
        return (Element) ApiInvoker.deserialize(response, "", Element.class);
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
  public TermMentionsResponse getTermMentions (String graphVertexId, String propertyKey, String propertyName) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || propertyKey == null || propertyName == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/termMentions".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphVertexId)))
      queryParams.put("graphVertexId", String.valueOf(graphVertexId));
    if(!"null".equals(String.valueOf(propertyKey)))
      queryParams.put("propertyKey", String.valueOf(propertyKey));
    if(!"null".equals(String.valueOf(propertyName)))
      queryParams.put("propertyName", String.valueOf(propertyName));
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
        return (TermMentionsResponse) ApiInvoker.deserialize(response, "", TermMentionsResponse.class);
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
  public DetectedObjects getDetectedObjects (String graphVertexId, String propertyName, String workspaceId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || propertyName == null || workspaceId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/detectedObjects".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphVertexId)))
      queryParams.put("graphVertexId", String.valueOf(graphVertexId));
    if(!"null".equals(String.valueOf(propertyName)))
      queryParams.put("propertyName", String.valueOf(propertyName));
    if(!"null".equals(String.valueOf(workspaceId)))
      queryParams.put("workspaceId", String.valueOf(workspaceId));
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
        return (DetectedObjects) ApiInvoker.deserialize(response, "", DetectedObjects.class);
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
  //error info- code: 404 reason: "Vertex not found" model: <none>
  public Element setVisibility (String graphVertexId, String visibilitySource) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || visibilitySource == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/visibility/set".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphVertexId)))
      queryParams.put("graphVertexId", String.valueOf(graphVertexId));
    if(!"null".equals(String.valueOf(visibilitySource)))
      queryParams.put("visibilitySource", String.valueOf(visibilitySource));
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
        return (Element) ApiInvoker.deserialize(response, "", Element.class);
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

