package io.lumify.web.clientapi.model;

import org.json.JSONObject;

public class GraphPosition {
    private final int x;
    private final int y;

    public GraphPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("x", getX());
        json.put("y", getY());
        return json;
    }
}
