package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.ApiInvoker;


import com.sun.jersey.multipart.FormDataMultiPart;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.util.*;

public class LongrunningprocessApi {
  protected String basePath = "http://lumify-dev:8889";
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

  //error info- code: 404 reason: "Long running process not found" model: <none>
  public String findByIdRaw (String longRunningProcessId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(longRunningProcessId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/long-running-process".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(longRunningProcessId)))
      queryParams.put("longRunningProcessId", String.valueOf(longRunningProcessId));
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
        return response;
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
  //error info- code: 404 reason: "Long running process not found" model: <none>
  public void cancel (String longRunningProcessId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(longRunningProcessId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/long-running-process/cancel".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(longRunningProcessId)))
      queryParams.put("longRunningProcessId", String.valueOf(longRunningProcessId));
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
        return ;
      }
      else {
        return ;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return ;
      }
      else {
        throw ex;
      }
    }
  }
  }

