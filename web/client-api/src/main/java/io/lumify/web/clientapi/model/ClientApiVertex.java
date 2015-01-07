package io.lumify.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("vertex")
public class ClientApiVertex extends ClientApiElement {
    private Double score;
    private List<String> edgeLabels = new ArrayList<String>();

    public List<String> getEdgeLabels() {
        return edgeLabels;
    }

    /**
     * search score
     */
    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public double getScore(double defaultValue) {
        if (this.score == null) {
            return defaultValue;
        }
        return this.score;
    }
}
