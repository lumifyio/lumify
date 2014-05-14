package io.lumify.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class NameValuePair {
    private final String value;
    private final String name;

    public NameValuePair(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getUrlEncodedName() {
        try {
            return URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getValue() {
        return value;
    }

    public String getUrlEncodedValue() {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
