package io.lumify.core.ingest.term.extraction;

import org.securegraph.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * An occurrence of a Term discovered by an extraction process.
 */
public class TermMention {

    private final int start;
    private final int end;
    private final String sign;
    private final String ontologyClassUri;
    private final boolean resolved;
    private final List<TermMentionProperty> newProperties;
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
            List<TermMentionProperty> newProperties,
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
        this.newProperties = newProperties;
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

    public List<TermMentionProperty> getNewProperties() {
        return newProperties;
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
        return String.format("{id: %s, sign: %s, loc: [%d, %d], ontology: %s, resolved? %s, useExisting? %s, process: %s}",
                id, sign, start, end, ontologyClassUri, resolved, useExisting, process);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + this.start;
        hash = 17 * hash + this.end;
        hash = 17 * hash + (this.sign != null ? this.sign.hashCode() : 0);
        hash = 17 * hash + (this.ontologyClassUri != null ? this.ontologyClassUri.hashCode() : 0);
        hash = 17 * hash + (this.resolved ? 1 : 0);
        hash = 17 * hash + (this.newProperties != null ? this.newProperties.hashCode() : 0);
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
        if (this.newProperties != other.newProperties && (this.newProperties == null || !this.newProperties.equals(other.newProperties))) {
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

    public static class TermMentionProperty {
        private final String key;
        private final String name;
        private final Object value;

        public TermMentionProperty(String key, String name, Object value) {
            this.key = key;
            this.name = name;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class Builder {

        private final String propertyKey;
        private final int start;
        private final int end;
        private String sign;
        private String ontologyClassUri;
        private boolean resolved;
        private List<TermMentionProperty> newProperties = new ArrayList<TermMentionProperty>();
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

        public Builder id(final Object id) {
            this.id = id;
            return this;
        }

        public Builder visibility(final Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder addProperty(final String key, final String name, final Object value) {
            this.newProperties.add(new TermMentionProperty(key, name, value));
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
            return new TermMention(start, end, sign, ontologyClassUri, propertyKey, resolved, newProperties,
                    useExisting, procBuilder.toString(), id, visibility);
        }
    }
}
