package com.altamiracorp.lumify.core.security;

import com.altamiracorp.securegraph.Visibility;

public class LumifyVisibility {
    public static final String VISIBILITY_STRING = "lumify";
    private final Visibility visibility;

    public LumifyVisibility() {
        this.visibility = new Visibility("");
    }

    public LumifyVisibility (String visibility) {
        if (visibility == null || visibility.length() == 0) {
            this.visibility = new Visibility("");
        } else {
            this.visibility = new Visibility("(" + visibility + ")|" + VISIBILITY_STRING);
        }
    }

    public Visibility getVisibility() {
        return visibility;
    }
}
