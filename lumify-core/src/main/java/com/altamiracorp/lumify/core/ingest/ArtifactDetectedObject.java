package com.altamiracorp.lumify.core.ingest;

public class ArtifactDetectedObject {
    private String concept;
    private String id;
    private double x1;
    private double y1;
    private double x2;
    private double y2;
    private String process;

    public ArtifactDetectedObject(double x1, double y1, double x2, double y2, String concept, String process) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.concept = concept;
        this.process = process;
    }

    public double getX1() {
        return x1;
    }

    public void setX1(long x1) {
        this.x1 = x1;
    }

    public double getY1() {
        return y1;
    }

    public void setY1(long y1) {
        this.y1 = y1;
    }

    public double getX2() {
        return x2;
    }

    public void setX2(long x2) {
        this.x2 = x2;
    }

    public double getY2() {
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

    public String getProcess () { return process; }

    public void setProcess (String process) { this.process = process; }

}
