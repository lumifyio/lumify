package io.lumify.http;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class URLBuilder {
    private final String baseUrl;
    private HttpRequestParameterBuilder requestParameterBuilder = new HttpRequestParameterBuilder();

    public URLBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void addParameter(String name, String value) {
        requestParameterBuilder.addParameter(name, value);
    }

    public String build() throws MalformedURLException {
        StringBuilder url = new StringBuilder(this.baseUrl);
        String requestParameters = requestParameterBuilder.build();
        if (requestParameters != null && requestParameters.length() > 0) {
            url.append("?").append(requestParameters);
        }
        return url.toString();
    }
}
