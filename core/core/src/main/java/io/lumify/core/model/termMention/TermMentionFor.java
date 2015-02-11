package io.lumify.core.model.termMention;

public enum TermMentionFor {
    VERTEX,
    PROPERTY,
    EDGE;

    public static int compare(TermMentionFor t1, TermMentionFor t2) {
        if (t1 == null && t2 == null) {
            return 0;
        }
        if (t1 != null && t2 == null) {
            return -1;
        }
        if (t1 == null) {
            return 1;
        }
        if (t1 == t2) {
            return 0;
        }
        return Integer.compare(t1.ordinal(), t2.ordinal());
    }
}
