package io.lumify.http;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class HttpPostMethod extends HttpMethod {
    public HttpPostMethod(URL baseUrl) {
        super(baseUrl);
    }

    @Override
    public HttpURLConnection openConnectionInternal() throws IOException {
        Charset utf8 = Charset.forName("UTF-8");
        byte[] data = getParameterString().getBytes(utf8);
        HttpURLConnection conn = (HttpURLConnection) getBaseUrl().openConnection();
        conn.setUseCaches(shouldUseCaches());
        conn.setInstanceFollowRedirects(shouldFollowRedirects());
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", utf8.toString());
        conn.setRequestProperty("Content-Length", Integer.toString(data.length));
        new DataOutputStream(conn.getOutputStream()).write(data);
        return conn;
    }
}
