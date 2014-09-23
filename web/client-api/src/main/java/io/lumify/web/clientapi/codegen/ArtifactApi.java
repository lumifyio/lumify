package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.ApiInvoker;

import io.lumify.web.clientapi.codegen.model.ArtifactImportResponse;
import com.sun.jersey.multipart.FormDataMultiPart;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.util.*;

public class ArtifactApi {
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

  //error info- code: 404 reason: "Artifact not found" model: <none>
  public String getHighlightedText (String graphVertexId, String propertyKey) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || propertyKey == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/artifact/highlightedText".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphVertexId)))
      queryParams.put("graphVertexId", String.valueOf(graphVertexId));
    if(!"null".equals(String.valueOf(propertyKey)))
      queryParams.put("propertyKey", String.valueOf(propertyKey));
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
        return (String) ApiInvoker.deserialize(response, "", String.class);
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
  public ArtifactImportResponse importFile (String visibilitySource, File file) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(visibilitySource == null || file == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/artifact/import".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    String[] contentTypes = {
      "multipart/form-data"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      hasFields = true;
      mp.field("visibilitySource", visibilitySource, MediaType.MULTIPART_FORM_DATA_TYPE);
      hasFields = true;
      com.sun.jersey.core.header.FormDataContentDisposition dispo = com.sun.jersey.core.header.FormDataContentDisposition
        .name("file")
        .fileName(file.getName())
        .size(file.length())
        .build();
      com.sun.jersey.multipart.FormDataBodyPart bodyPart = new com.sun.jersey.multipart.FormDataBodyPart(dispo, file, MediaType.MULTIPART_FORM_DATA_TYPE);
      mp.bodyPart(bodyPart);
      if(hasFields)
        postBody = mp;
    }
    else {
      formParams.put("visibilitySource", visibilitySource);}

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ArtifactImportResponse) ApiInvoker.deserialize(response, "", ArtifactImportResponse.class);
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

