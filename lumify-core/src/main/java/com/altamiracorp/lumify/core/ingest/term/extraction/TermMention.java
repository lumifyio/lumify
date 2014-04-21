package com.altamiracorp.lumify.core.ingest.term.extraction;

import com.altamiracorp.securegraph.Visibility;

import java.util.*;

/**
 * An occurrence of a Term discovered by an extraction process.
 */
public class TermMention {

    private final int start;
    private final int end;
    private final String sign;
    private final String ontologyClassUri;
    private final boolean resolved;
    private final Map<String, Object> properties;
    private final boolean useExisting;
    private final String propertyKey;
    private String process = "";
    private Object id;
    private Visibility visibility;

    private TermMention(
            int start,
            int end,
            String sign,
            String ontologyClassUri,
            String propertyKey,
            boolean resolved,
            Map<String, Object> propertyValue,
            boolean useExisting,
            String process,
            Object id,
            Visibility visibility) {
        this.start = start;
        this.end = end;
        this.sign = sign;
        this.ontologyClassUri = ontologyClassUri;
        this.propertyKey = propertyKey;
        this.resolved = resolved;
        this.properties = propertyValue;
        this.useExisting = useExisting;
        this.process = process;
        this.id = id;
        this.visibility = visibility;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getSign() {
        return sign;
    }

    public String getOntologyClassUri() {
        return ontologyClassUri;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public Object getId() {
        return id;
    }

    public boolean isResolved() {
        return resolved;
    }

    public Map<String, Object> getPropertyValue() {
        return properties;
    }

    public boolean getUseExisting() {
        return useExisting;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        return String.format("{id: %s, sign: %s, loc: [%d, %d], ontology: %s, resolved? %s, useExisting? %s, props: %s, process: %s}",
                id, sign, start, end, ontologyClassUri, resolved, useExisting, properties, process);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + this.start;
        hash = 17 * hash + this.end;
        hash = 17 * hash + (this.sign != null ? this.sign.hashCode() : 0);
        hash = 17 * hash + (this.ontologyClassUri != null ? this.ontologyClassUri.hashCode() : 0);
        hash = 17 * hash + (this.resolved ? 1 : 0);
        hash = 17 * hash + (this.properties != null ? this.properties.hashCode() : 0);
        hash = 17 * hash + (this.useExisting ? 1 : 0);
        hash = 17 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TermMention other = (TermMention) obj;
        if (this.start != other.start) {
            return false;
        }
        if (this.end != other.end) {
            return false;
        }
        if ((this.sign == null) ? (other.sign != null) : !this.sign.equals(other.sign)) {
            return false;
        }
        if ((this.ontologyClassUri == null) ? (other.ontologyClassUri != null) : !this.ontologyClassUri.equals(other.ontologyClassUri)) {
            return false;
        }
        if (this.resolved != other.resolved) {
            return false;
        }
        if (this.properties != other.properties && (this.properties == null || !this.properties.equals(other.properties))) {
            return false;
        }
        if (this.useExisting != other.useExisting) {
            return false;
        }
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    public static class Builder {

        private final String propertyKey;
        private final int start;
        private final int end;
        private String sign;
        private String ontologyClassUri;
        private boolean resolved;
        private Map<String, Object> properties = new HashMap<String, Object>();
        private boolean useExisting;
        private final List<String> process = new ArrayList<String>();
        private Object id;
        private Visibility visibility;

        public Builder(int start, int end, String sign, String ontologyClassUri, String propertyKey, Visibility visibility) {
            this.start = start;
            this.end = end;
            this.sign = sign;
            this.ontologyClassUri = ontologyClassUri;
            this.propertyKey = propertyKey;
            this.visibility = visibility;
        }

        public Builder(final TermMention tm) {
            this.start = tm.start;
            this.end = tm.end;
            this.sign = tm.sign;
            this.ontologyClassUri = tm.ontologyClassUri;
            this.propertyKey = tm.propertyKey;
            this.resolved = tm.resolved;
            this.properties = new HashMap<String, Object>(tm.properties);
            this.useExisting = tm.useExisting;
            this.process.add(tm.process);
            this.id = tm.id;
            this.visibility = tm.visibility;
        }

        public Builder resolved(final boolean resolved) {
            this.resolved = resolved;
            return this;
        }

        public Builder useExisting(final boolean useExisting) {
            this.useExisting = useExisting;
            return this;
        }

        public Builder sign(final String sign) {
            this.sign = sign;
            return this;
        }

        public Builder ontologyClassUri(final String ontologyClassUri) {
            this.ontologyClassUri = ontologyClassUri;
            return this;
        }

        public Builder process(final String process) {
            this.process.add(process);
            return this;
        }

        public Builder properties(final Map<String, Object> map) {
            this.properties = map != null ? new HashMap<String, Object>(map) : new HashMap<String, Object>();
            return this;
        }

        public Builder id(final Object id) {
            this.id = id;
            return this;
        }

        public Builder visibility(final Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder setProperty(final String key, final Object value) {
            if (value == null) {
                this.properties.remove(key);
            } else {
                this.properties.put(key, value);
            }
            return this;
        }

        public TermMention build() {
            StringBuilder procBuilder = new StringBuilder();
            for (String proc : process) {
                if (procBuilder.length() > 0) {
                    procBuilder.append("; ");
                }
                procBuilder.append(proc);
            }
            return new TermMention(start, end, sign, ontologyClassUri, propertyKey, resolved, Collections.unmodifiableMap(properties),
                    useExisting, procBuilder.toString(), id, visibility);
        }
    }
}
