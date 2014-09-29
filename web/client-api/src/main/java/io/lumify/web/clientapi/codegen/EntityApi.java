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
  public void unresolveTerm (String graphVertexId, String propertyKey, Integer mentionStart, Integer mentionEnd, String sign, String conceptId, String edgeId, String rowKey) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || propertyKey == null || mentionStart == null || mentionEnd == null || sign == null || conceptId == null || edgeId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/entity/unresolveTerm".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphVertexId)))
      queryParams.put("graphVertexId", String.valueOf(graphVertexId));
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
    if(!"null".equals(String.valueOf(edgeId)))
      queryParams.put("edgeId", String.valueOf(edgeId));
    if(!"null".equals(String.valueOf(rowKey)))
      queryParams.put("rowKey", String.valueOf(rowKey));
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
  public void resolveDetectedObject (String artifactId, String title, String conceptId, String visibilitySource, String graphVertexId, String justificationText, String sourceInfo, String originalPropertyKey, Double x1, Double x2, Double y1, Double y2) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(artifactId == null || title == null || conceptId == null || visibilitySource == null || graphVertexId == null || justificationText == null || sourceInfo == null || originalPropertyKey == null || x1 == null || x2 == null || y1 == null || y2 == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/entity/resolveDetectedObject".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(artifactId)))
      queryParams.put("artifactId", String.valueOf(artifactId));
    if(!"null".equals(String.valueOf(title)))
      queryParams.put("title", String.valueOf(title));
    if(!"null".equals(String.valueOf(conceptId)))
      queryParams.put("conceptId", String.valueOf(conceptId));
    if(!"null".equals(String.valueOf(visibilitySource)))
      queryParams.put("visibilitySource", String.valueOf(visibilitySource));
    if(!"null".equals(String.valueOf(graphVertexId)))
      queryParams.put("graphVertexId", String.valueOf(graphVertexId));
    if(!"null".equals(String.valueOf(justificationText)))
      queryParams.put("justificationText", String.valueOf(justificationText));
    if(!"null".equals(String.valueOf(sourceInfo)))
      queryParams.put("sourceInfo", String.valueOf(sourceInfo));
    if(!"null".equals(String.valueOf(originalPropertyKey)))
      queryParams.put("originalPropertyKey", String.valueOf(originalPropertyKey));
    if(!"null".equals(String.valueOf(x1)))
      queryParams.put("x1", String.valueOf(x1));
    if(!"null".equals(String.valueOf(x2)))
      queryParams.put("x2", String.valueOf(x2));
    if(!"null".equals(String.valueOf(y1)))
      queryParams.put("y1", String.valueOf(y1));
    if(!"null".equals(String.valueOf(y2)))
      queryParams.put("y2", String.valueOf(y2));
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
  public void unresolveDetectedObject (String vertexId, String multiValueKey) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(vertexId == null || multiValueKey == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/entity/unresolveDetectedObject".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(vertexId)))
      queryParams.put("vertexId", String.valueOf(vertexId));
    if(!"null".equals(String.valueOf(multiValueKey)))
      queryParams.put("multiValueKey", String.valueOf(multiValueKey));
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

