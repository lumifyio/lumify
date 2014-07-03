package io.lumify.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpGetMethod extends HttpMethod {

    public HttpGetMethod(URL baseUrl) {
        super(baseUrl);
    }

    @Override
    public HttpURLConnection openConnectionInternal() throws IOException {
        String url = getBaseUrl().toString();
        String params = getParameterString();

        if (params != null && params.length() > 0) {
            url = url + "?" + params;
        }

        URL getUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) getUrl.openConnection();
        conn.setUseCaches(shouldUseCaches());
        conn.setInstanceFollowRedirects(shouldFollowRedirects());
        return conn;
    }
}
