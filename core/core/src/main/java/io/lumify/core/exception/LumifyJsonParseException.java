package io.lumify.core.exception;

import org.json.JSONException;

public class LumifyJsonParseException extends RuntimeException {
    public LumifyJsonParseException(String jsonString, JSONException cause) {
        super("Could not parse json string: " + jsonString, cause);
    }
}
