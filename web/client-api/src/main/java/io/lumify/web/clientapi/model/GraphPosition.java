package io.lumify.web.clientapi.model;

public class GraphPosition {
    private int x;
    private int y;

    public GraphPosition() {

    }

    public GraphPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "GraphPosition{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
