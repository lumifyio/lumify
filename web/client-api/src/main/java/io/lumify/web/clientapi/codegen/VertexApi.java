package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.ApiInvoker;

import io.lumify.web.clientapi.model.ClientApiLongRunningProcessSubmitResponse;
import io.lumify.web.clientapi.model.ClientApiDetectedObjects;
import io.lumify.web.clientapi.model.ClientApiVertexFindRelatedResponse;
import io.lumify.web.clientapi.model.ClientApiVerticesExistsResponse;
import io.lumify.web.clientapi.model.ClientApiVertexSearchResponse;
import io.lumify.web.clientapi.model.ClientApiElement;
import io.lumify.web.clientapi.model.ClientApiVertexEdges;
import io.lumify.web.clientapi.model.ClientApiArtifactImportResponse;
import io.lumify.web.clientapi.model.ClientApiVertexMultipleResponse;
import io.lumify.web.clientapi.model.ClientApiTermMentionsResponse;
import com.sun.jersey.multipart.FormDataMultiPart;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.util.*;

public class VertexApi {
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

  //error info- code: 404 reason: "Vertex not found" model: <none>
  public ClientApiElement getByVertexId (String graphVertexId) throws ApiException {
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
        return (ClientApiElement) ApiInvoker.deserialize(response, "", ClientApiElement.class);
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
  public ClientApiVertexEdges getEdges (String graphVertexId, String edgeLabel, Integer offset, Integer size) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/edges".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphVertexId)))
      queryParams.put("graphVertexId", String.valueOf(graphVertexId));
    if(!"null".equals(String.valueOf(edgeLabel)))
      queryParams.put("edgeLabel", String.valueOf(edgeLabel));
    if(!"null".equals(String.valueOf(offset)))
      queryParams.put("offset", String.valueOf(offset));
    if(!"null".equals(String.valueOf(size)))
      queryParams.put("size", String.valueOf(size));
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
        return (ClientApiVertexEdges) ApiInvoker.deserialize(response, "", ClientApiVertexEdges.class);
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
  public ClientApiElement create (String conceptType, String visibilitySource) throws ApiException {
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
        return (ClientApiElement) ApiInvoker.deserialize(response, "", ClientApiElement.class);
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
  public ClientApiElement setProperty (String graphVertexId, String propertyKey, String propertyName, String value, String visibilitySource, String justificationText, String sourceInfo, String metadata) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || propertyKey == null || propertyName == null || value == null || visibilitySource == null || justificationText == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/property".replaceAll("\\{format\\}","json");

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
        return (ClientApiElement) ApiInvoker.deserialize(response, "", ClientApiElement.class);
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
  public void deleteProperty (String graphVertexId, String propertyKey, String propertyName) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || propertyKey == null || propertyName == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/property".replaceAll("\\{format\\}","json");

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
      String response = apiInvoker.invokeAPI(basePath, path, "DELETE", queryParams, postBody, headerParams, formParams, contentType);
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
  public void deleteEdge (String edgeId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(edgeId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/edge".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(edgeId)))
      queryParams.put("edgeId", String.valueOf(edgeId));
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
      String response = apiInvoker.invokeAPI(basePath, path, "DELETE", queryParams, postBody, headerParams, formParams, contentType);
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
  public void deleteVertex (String graphVertexId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex".replaceAll("\\{format\\}","json");

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
      String response = apiInvoker.invokeAPI(basePath, path, "DELETE", queryParams, postBody, headerParams, formParams, contentType);
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
  public ClientApiTermMentionsResponse getTermMentions (String graphVertexId, String propertyKey, String propertyName) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || propertyKey == null || propertyName == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/term-mentions".replaceAll("\\{format\\}","json");

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
        return (ClientApiTermMentionsResponse) ApiInvoker.deserialize(response, "", ClientApiTermMentionsResponse.class);
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
  public ClientApiDetectedObjects getDetectedObjects (String graphVertexId, String propertyName, String workspaceId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || propertyName == null || workspaceId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/detected-objects".replaceAll("\\{format\\}","json");

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
        return (ClientApiDetectedObjects) ApiInvoker.deserialize(response, "", ClientApiDetectedObjects.class);
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
  public ClientApiElement setVisibility (String graphVertexId, String visibilitySource) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || visibilitySource == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/visibility".replaceAll("\\{format\\}","json");

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
        return (ClientApiElement) ApiInvoker.deserialize(response, "", ClientApiElement.class);
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
  //error info- code: 404 reason: "Artifact not found" model: <none>
  public String getHighlightedText (String graphVertexId, String propertyKey) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexId == null || propertyKey == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/highlighted-text".replaceAll("\\{format\\}","json");

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
  public ClientApiArtifactImportResponse importFile (String visibilitySource, File file) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(visibilitySource == null || file == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/import".replaceAll("\\{format\\}","json");

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
        return (ClientApiArtifactImportResponse) ApiInvoker.deserialize(response, "", ClientApiArtifactImportResponse.class);
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
  public void resolveTerm (String artifactId, String propertyKey, Integer mentionStart, Integer mentionEnd, String sign, String conceptId, String visibilitySource, String resolvedVertexId, String justificationText, String sourceInfo) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(artifactId == null || propertyKey == null || mentionStart == null || mentionEnd == null || sign == null || conceptId == null || visibilitySource == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/resolve-term".replaceAll("\\{format\\}","json");

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
  public void unresolveTerm (String termMentionId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(termMentionId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/unresolve-term".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(termMentionId)))
      queryParams.put("termMentionId", String.valueOf(termMentionId));
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
    if(artifactId == null || title == null || conceptId == null || visibilitySource == null || x1 == null || x2 == null || y1 == null || y2 == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/resolve-detected-object".replaceAll("\\{format\\}","json");

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
    String path = "/vertex/unresolve-detected-object".replaceAll("\\{format\\}","json");

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
  public ClientApiVertexSearchResponse vertexSearch (String q, String filter, Integer offset, Integer size, String conceptType, Boolean leafNodes, List<String> relatedToVertexIds) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(filter == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/search".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(q)))
      queryParams.put("q", String.valueOf(q));
    if(!"null".equals(String.valueOf(filter)))
      queryParams.put("filter", String.valueOf(filter));
    if(!"null".equals(String.valueOf(offset)))
      queryParams.put("offset", String.valueOf(offset));
    if(!"null".equals(String.valueOf(size)))
      queryParams.put("size", String.valueOf(size));
    if(!"null".equals(String.valueOf(conceptType)))
      queryParams.put("conceptType", String.valueOf(conceptType));
    if(!"null".equals(String.valueOf(leafNodes)))
      queryParams.put("leafNodes", String.valueOf(leafNodes));
    String[] contentTypes = {
      "multipart/form-data"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      hasFields = true;
      if(relatedToVertexIds != null) { for(String relatedToVertexId:relatedToVertexIds) { mp.field("relatedToVertexIds[]", relatedToVertexId, MediaType.MULTIPART_FORM_DATA_TYPE); } }
      if(hasFields && !mp.getFields().isEmpty())
        postBody = mp;
    }
    else {
      throw new java.lang.RuntimeException("invalid content type");}

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiVertexSearchResponse) ApiInvoker.deserialize(response, "", ClientApiVertexSearchResponse.class);
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
  public ClientApiVertexSearchResponse vertexGeoSearch (Double lat, Double lon, Double radius) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(lat == null || lon == null || radius == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/geo-search".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(lat)))
      queryParams.put("lat", String.valueOf(lat));
    if(!"null".equals(String.valueOf(lon)))
      queryParams.put("lon", String.valueOf(lon));
    if(!"null".equals(String.valueOf(radius)))
      queryParams.put("radius", String.valueOf(radius));
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
        return (ClientApiVertexSearchResponse) ApiInvoker.deserialize(response, "", ClientApiVertexSearchResponse.class);
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
  public ClientApiLongRunningProcessSubmitResponse findPath (String sourceGraphVertexId, String destGraphVertexId, Integer hops) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(sourceGraphVertexId == null || destGraphVertexId == null || hops == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/find-path".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(sourceGraphVertexId)))
      queryParams.put("sourceGraphVertexId", String.valueOf(sourceGraphVertexId));
    if(!"null".equals(String.valueOf(destGraphVertexId)))
      queryParams.put("destGraphVertexId", String.valueOf(destGraphVertexId));
    if(!"null".equals(String.valueOf(hops)))
      queryParams.put("hops", String.valueOf(hops));
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
        return (ClientApiLongRunningProcessSubmitResponse) ApiInvoker.deserialize(response, "", ClientApiLongRunningProcessSubmitResponse.class);
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
  public ClientApiVertexFindRelatedResponse findRelated (List<String> graphVertexIds, String limitParentConceptId, String limitEdgeLabel, Integer maxVerticesToReturn) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphVertexIds == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/find-related".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(limitParentConceptId)))
      queryParams.put("limitParentConceptId", String.valueOf(limitParentConceptId));
    if(!"null".equals(String.valueOf(limitEdgeLabel)))
      queryParams.put("limitEdgeLabel", String.valueOf(limitEdgeLabel));
    if(!"null".equals(String.valueOf(maxVerticesToReturn)))
      queryParams.put("maxVerticesToReturn", String.valueOf(maxVerticesToReturn));
    String[] contentTypes = {
      "multipart/form-data"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      hasFields = true;
      if(graphVertexIds != null) { for(String graphVertexId:graphVertexIds) { mp.field("graphVertexIds[]", graphVertexId, MediaType.MULTIPART_FORM_DATA_TYPE); } }
      if(hasFields && !mp.getFields().isEmpty())
        postBody = mp;
    }
    else {
      throw new java.lang.RuntimeException("invalid content type");}

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiVertexFindRelatedResponse) ApiInvoker.deserialize(response, "", ClientApiVertexFindRelatedResponse.class);
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
  public ClientApiVertexMultipleResponse findMultiple (List<String> vertexIds, Boolean fallbackToPublic) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(vertexIds == null || fallbackToPublic == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/multiple".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(fallbackToPublic)))
      queryParams.put("fallbackToPublic", String.valueOf(fallbackToPublic));
    String[] contentTypes = {
      "multipart/form-data"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      hasFields = true;
      if(vertexIds != null) { for(String vertexId:vertexIds) { mp.field("vertexIds[]", vertexId, MediaType.MULTIPART_FORM_DATA_TYPE); } }
      if(hasFields && !mp.getFields().isEmpty())
        postBody = mp;
    }
    else {
      throw new java.lang.RuntimeException("invalid content type");}

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiVertexMultipleResponse) ApiInvoker.deserialize(response, "", ClientApiVertexMultipleResponse.class);
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
  public ClientApiVerticesExistsResponse doExist (List<String> vertexIds) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(vertexIds == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/vertex/exists".replaceAll("\\{format\\}","json");

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
      if(vertexIds != null) { for(String vertexId:vertexIds) { mp.field("vertexIds[]", vertexId, MediaType.MULTIPART_FORM_DATA_TYPE); } }
      if(hasFields && !mp.getFields().isEmpty())
        postBody = mp;
    }
    else {
      throw new java.lang.RuntimeException("invalid content type");}

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiVerticesExistsResponse) ApiInvoker.deserialize(response, "", ClientApiVerticesExistsResponse.class);
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

