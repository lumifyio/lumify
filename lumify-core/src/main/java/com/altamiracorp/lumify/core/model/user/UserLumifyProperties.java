package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.model.properties.ByteArrayLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.TextLumifyProperty;
import com.altamiracorp.securegraph.TextIndexHint;
import com.altamiracorp.securegraph.Vertex;

import java.util.HashSet;
import java.util.Set;

public final class UserLumifyProperties {
    public static final TextLumifyProperty USERNAME = new TextLumifyProperty("http://lumify.io/user/username", TextIndexHint.EXACT_MATCH);
    public static final TextLumifyProperty AUTHORIZATIONS = new TextLumifyProperty("http://lumify.io/user/authorizations", TextIndexHint.NONE);
    public static final TextLumifyProperty STATUS = new TextLumifyProperty("http://lumify.io/user/status", TextIndexHint.NONE);
    public static final TextLumifyProperty CURRENT_WORKSPACE = new TextLumifyProperty("http://lumify.io/user/currentWorkspace", TextIndexHint.EXACT_MATCH);
    public static final ByteArrayLumifyProperty PASSWORD_SALT = new ByteArrayLumifyProperty("http://lumify.io/user/passwordSalt");
    public static final ByteArrayLumifyProperty PASSWORD_HASH = new ByteArrayLumifyProperty("http://lumify.io/user/passwordHash");

    public static Set<String> getAuthorizations(Vertex userVertex) {
        String authorizationsString = AUTHORIZATIONS.getPropertyValue(userVertex);
        if (authorizationsString == null) {
            return new HashSet<String>();
        }
        String[] authorizationsArray = authorizationsString.split(",");
        if (authorizationsArray.length == 1 && authorizationsArray[0].length() == 0) {
            authorizationsArray = new String[0];
        }
        HashSet<String> authorizations = new HashSet<String>();
        for (String s : authorizationsArray) {
            // Accumulo doesn't like zero length strings. they shouldn't be in the auth string to begin with but this just protects from that happening.
            if (s.trim().length() == 0) {
                continue;
            }

            authorizations.add(s);
        }
        return authorizations;
    }
}
