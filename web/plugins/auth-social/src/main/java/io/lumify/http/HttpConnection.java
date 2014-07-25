package io.lumify.http;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class HttpConnection {
    private final HttpURLConnection httpURLConnection;

    HttpConnection(HttpURLConnection httpURLConnection) {
        this.httpURLConnection = httpURLConnection;
    }

    public String getResponseAsString() throws IOException {
        return IOUtils.toString(getResponseInputStream(), "UTF-8");
    }

    public InputStream getResponseInputStream() throws IOException {
        return httpURLConnection.getInputStream();
    }

    public int getResponseCode() throws IOException {
        return httpURLConnection.getResponseCode();
    }

    public String getResponseMessage() throws IOException {
        return httpURLConnection.getResponseMessage();
    }
}
