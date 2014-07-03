package io.lumify.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class HttpMethod {
    private final URL baseUrl;
    private HttpRequestParameterBuilder requestParameterBuilder = new HttpRequestParameterBuilder();
    private List<NameValuePair> headers = new ArrayList<NameValuePair>();
    private boolean useCaches = false;
    private boolean followRedirects = true;

    public HttpMethod(URL baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean shouldUseCaches() {
        return useCaches;
    }

    public void setUseCaches(boolean useCaches) {
        this.useCaches = useCaches;
    }

    public boolean shouldFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public void addRequestParameter(String name, String value) {
        addRequestParameter(new NameValuePair(name, value));
    }

    public void addRequestParameter(NameValuePair parameter) {
        requestParameterBuilder.addParameter(parameter);
    }

    public void setHeader(String name, String value) {
        setHeader(new NameValuePair(name, value));
    }

    public void setHeader(NameValuePair header) {
        headers.add(header);
    }

    public HttpConnection openConnection() throws IOException {
        HttpURLConnection connection = openConnectionInternal();
        setRequestHeaders(connection);
        return new HttpConnection(connection);
    }

    protected abstract HttpURLConnection openConnectionInternal() throws IOException;

    protected URL getBaseUrl() {
        return baseUrl;
    }

    protected String getParameterString() {
        return requestParameterBuilder.build();
    }

    private void setRequestHeaders(HttpURLConnection connection) {
        for (NameValuePair pair : this.headers) {
            connection.setRequestProperty(pair.getName(), pair.getValue());
        }
    }
}
