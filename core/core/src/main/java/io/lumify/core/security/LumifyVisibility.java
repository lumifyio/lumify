package io.lumify.core.security;

import org.securegraph.Visibility;

public class LumifyVisibility {
    public static final String SUPER_USER_VISIBILITY_STRING = "lumify";
    private final Visibility visibility;

    public LumifyVisibility() {
        this.visibility = new Visibility("");
    }

    public LumifyVisibility(String visibility) {
        if (visibility == null || visibility.length() == 0) {
            this.visibility = new Visibility("");
        } else {
            this.visibility = new Visibility("(" + visibility + ")|" + SUPER_USER_VISIBILITY_STRING);
        }
    }

    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        return getVisibility().toString();
    }

    public static Visibility and(Visibility visibility, String additionalVisibility) {
        if (visibility.getVisibilityString().length() == 0) {
            return new Visibility(additionalVisibility);
        }
        return new Visibility("(" + visibility.getVisibilityString() + ")&(" + additionalVisibility + ")");
    }
}
