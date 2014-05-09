package io.lumify.web;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class URLBuilder {
    private final String baseUrl;
    private List<NameValuePair> params = new ArrayList<NameValuePair>();

    public URLBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void addParameter(String name, String value) {
        params.add(new NameValuePair(name, value));
    }

    public URL build() throws MalformedURLException {
        StringBuilder url = new StringBuilder(this.baseUrl);
        boolean isFirstParameter = true;
        for (NameValuePair pair : params) {
            if (isFirstParameter) {
                url.append("?");
                isFirstParameter = false;
            } else {
                url.append("&");
            }

            try {
                url.append(URLEncoder.encode(pair.getName(), "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        return new URL(url.toString());
    }

    private class NameValuePair {
        private final String value;
        private final String name;

        public String getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public NameValuePair(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
