package io.lumify.web.clientapi.model;

public class ClientApiEdgeWithVertexData extends ClientApiEdge {
    private ClientApiVertex source;
    private ClientApiVertex target;

    public ClientApiVertex getSource() {
        return source;
    }

    public void setSource(ClientApiVertex source) {
        this.source = source;
    }

    public ClientApiVertex getTarget() {
        return target;
    }

    public void setTarget(ClientApiVertex target) {
        this.target = target;
    }
}
