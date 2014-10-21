package io.lumify.web.clientapi.model;

public class ClientApiLongRunningProcessSubmitResponse implements ClientApiObject {
    private String id;

    public ClientApiLongRunningProcessSubmitResponse() {

    }

    public ClientApiLongRunningProcessSubmitResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
