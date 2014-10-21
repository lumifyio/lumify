package io.lumify.web.clientapi.model;

public class ClientApiLongRunningProcessResponse implements ClientApiObject {
    private String id;

    public ClientApiLongRunningProcessResponse() {

    }

    public ClientApiLongRunningProcessResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
