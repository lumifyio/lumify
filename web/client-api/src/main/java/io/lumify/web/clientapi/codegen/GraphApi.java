package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.ApiInvoker;

import io.lumify.web.clientapi.codegen.model.GraphVertexSearchResult;
import com.sun.jersey.multipart.FormDataMultiPart;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.util.*;

public class GraphApi {
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

  public GraphVertexSearchResult vertexSearch (String q, String filter, Integer offset, Integer size, String conceptType, Boolean leafNodes, String relatedToVertexId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(filter == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/graph/vertex/search".replaceAll("\\{format\\}","json");

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
    if(!"null".equals(String.valueOf(relatedToVertexId)))
      queryParams.put("relatedToVertexId", String.valueOf(relatedToVertexId));
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
        return (GraphVertexSearchResult) ApiInvoker.deserialize(response, "", GraphVertexSearchResult.class);
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

