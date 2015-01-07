package io.lumify.csv.model;

import io.lumify.web.clientapi.model.util.ClientApiConverter;

import java.util.ArrayList;
import java.util.List;

public class Mapping {
    private int linesToSkip;
    private List<Vertex> vertices = new ArrayList<Vertex>();
    private List<Edge> edges = new ArrayList<Edge>();

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public int getLinesToSkip() {
        return linesToSkip;
    }

    public void setLinesToSkip(int linesToSkip) {
        this.linesToSkip = linesToSkip;
    }

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    public static class Vertex {
        private List<Property> properties = new ArrayList<Property>();

        public List<Property> getProperties() {
            return properties;
        }

        @Override
        public String toString() {
            return ClientApiConverter.clientApiToString(this);
        }
    }

    public static class Edge {
        private String label;
        private int in;
        private int out;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getIn() {
            return in;
        }

        public void setIn(int in) {
            this.in = in;
        }

        public int getOut() {
            return out;
        }

        public void setOut(int out) {
            this.out = out;
        }

        @Override
        public String toString() {
            return ClientApiConverter.clientApiToString(this);
        }
    }

    public static class Property {
        private Integer column;
        private String name;
        private String key;
        private Object value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Integer getColumn() {
            return column;
        }

        public void setColumn(Integer column) {
            this.column = column;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return ClientApiConverter.clientApiToString(this);
        }
    }
}
