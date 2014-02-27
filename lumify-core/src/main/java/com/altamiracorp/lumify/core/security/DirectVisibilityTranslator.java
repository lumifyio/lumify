package com.altamiracorp.lumify.core.security;

import com.altamiracorp.securegraph.Visibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DirectVisibilityTranslator implements VisibilityTranslator {
    public void init(Map configuration) {

    }

    @Override
    public Visibility toVisibility(String source, String... additionalRequiredVisibilities) {
        StringBuilder visibilityString = new StringBuilder();
        List<String> all = new ArrayList<String>();

        if (source != null && source.trim().length() > 0) {
            all.add(source.trim());
        }

        Collections.addAll(all, additionalRequiredVisibilities);

        for (int i = 0; i < all.size(); i++) {
            String additionalRequiredVisibility = all.get(i);
            if (i > 0) {
                visibilityString.append("&");
            }
            visibilityString
                    .append("(")
                    .append(additionalRequiredVisibility)
                    .append(")");
        }
        return new Visibility(visibilityString.toString());
    }

    @Override
    public Visibility toVisibilityWithWorkspace(String source, String workspaceId) {
        String[] additionalRequiredVisibilities;
        if (workspaceId == null) {
            additionalRequiredVisibilities = new String[0];
        } else {
            additionalRequiredVisibilities = new String[]{workspaceId};
        }
        return toVisibility(source, additionalRequiredVisibilities);
    }
}
