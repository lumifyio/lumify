package com.altamiracorp.lumify.core.ingest;

public class ArtifactDetectedObject {
    private String concept;
    private String id;
    private long x1;
    private long y1;
    private long x2;
    private long y2;

    public ArtifactDetectedObject(long x1, long y1, long x2, long y2, String concept) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.concept = concept;
    }

    public long getX1() {
        return x1;
    }

    public void setX1(long x1) {
        this.x1 = x1;
    }

    public long getY1() {
        return y1;
    }

    public void setY1(long y1) {
        this.y1 = y1;
    }

    public long getX2() {
        return x2;
    }

    public void setX2(long x2) {
        this.x2 = x2;
    }

    public long getY2() {
        return y2;
    }

    public void setY2(long y2) {
        this.y2 = y2;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getId () { return id; }

    public void setId (String id) { this.id = id; }

}
