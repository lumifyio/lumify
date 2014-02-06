package com.altamiracorp.lumify.core.security;

import com.altamiracorp.securegraph.Visibility;

import java.util.Map;

public class DirectVisibilityTranslator implements VisibilityTranslator {
    public void init(Map configuration) {

    }


    @Override
    public Visibility toVisibility(String source) {
        return new Visibility(source);
    }
}
