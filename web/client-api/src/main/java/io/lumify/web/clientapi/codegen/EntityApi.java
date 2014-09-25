package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.ApiInvoker;

import com.sun.jersey.multipart.FormDataMultiPart;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.util.*;

public class EntityApi {
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

  public void resolveTerm (String artifactId, String propertyKey, Integer mentionStart, Integer mentionEnd, String sign, String conceptId, String visibilitySource, String resolvedVertexId, String justificationText, String sourceInfo) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(artifactId == null || propertyKey == null || mentionStart == null || mentionEnd == null || sign == null || conceptId == null || visibilitySource == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/entity/resolveTerm".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(artifactId)))
      queryParams.put("artifactId", String.valueOf(artifactId));
    if(!"null".equals(String.valueOf(propertyKey)))
      queryParams.put("propertyKey", String.valueOf(propertyKey));
    if(!"null".equals(String.valueOf(mentionStart)))
      queryParams.put("mentionStart", String.valueOf(mentionStart));
    if(!"null".equals(String.valueOf(mentionEnd)))
      queryParams.put("mentionEnd", String.valueOf(mentionEnd));
    if(!"null".equals(String.valueOf(sign)))
      queryParams.put("sign", String.valueOf(sign));
    if(!"null".equals(String.valueOf(conceptId)))
      queryParams.put("conceptId", String.valueOf(conceptId));
    if(!"null".equals(String.valueOf(visibilitySource)))
      queryParams.put("visibilitySource", String.valueOf(visibilitySource));
    if(!"null".equals(String.valueOf(resolvedVertexId)))
      queryParams.put("resolvedVertexId", String.valueOf(resolvedVertexId));
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

