package io.lumify.web.clientapi;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public abstract class LumifyHttpURLConnectionWebClient extends LumifyWebClient {
    private final String baseUrl;
    private String jsessionid;

    public LumifyHttpURLConnectionWebClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    protected HttpResponse httpGet(String uri) {
        ensureLoggedIn();
        try {
            URL url = createUrlFromUri(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.addRequestProperty("Cookie", "JSESSIONID=" + this.jsessionid);
            if (getCurrentWorkspaceId() != null) {
                conn.addRequestProperty("Lumify-Workspace-Id", getCurrentWorkspaceId());
            }
            return new HttpURLConnectionHttpResponse(conn);
        } catch (Exception ex) {
            throw new LumifyClientApiException("httpGet failed", ex);
        }
    }

    @Override
    protected HttpResponse httpPost(String uri, Map<String, List<String>> additionalHeaders, InputStream content) {
        ensureLoggedIn();
        try {
            URL url = createUrlFromUri(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.addRequestProperty("Cookie", "JSESSIONID=" + this.jsessionid);
            conn.addRequestProperty("Lumify-CSRF-Token", getCsrfToken());
            if (getCurrentWorkspaceId() != null) {
                conn.addRequestProperty("Lumify-Workspace-Id", getCurrentWorkspaceId());
            }
            if (additionalHeaders != null) {
                for (Map.Entry<String, List<String>> additionalHeaderEntry : additionalHeaders.entrySet()) {
                    for (String value : additionalHeaderEntry.getValue()) {
                        conn.addRequestProperty(additionalHeaderEntry.getKey(), value);
                    }
                }
            }
            if (content != null) {
                conn.setDoOutput(true);
                IOUtils.copy(content, conn.getOutputStream());
            }
            return new HttpURLConnectionHttpResponse(conn);
        } catch (Exception ex) {
            throw new LumifyClientApiException("httpPost failed", ex);
        }
    }

    protected URL createUrlFromUri(String uri) throws MalformedURLException {
        String urlString = baseUrl;
        if (baseUrl.endsWith("/") && uri.startsWith("/")) {
            urlString += uri.substring(1);
        } else {
            urlString += uri;
        }
        return new URL(urlString);
    }

    private void ensureLoggedIn() {
        if (jsessionid != null) {
            return;
        }
        jsessionid = logIn();
    }

    protected abstract String logIn();

    private class HttpURLConnectionHttpResponse extends HttpResponse {
        private final HttpURLConnection conn;

        public HttpURLConnectionHttpResponse(HttpURLConnection conn) {
            this.conn = conn;
        }

        @Override
        public InputStream getInputStream() {
            try {
                return this.conn.getInputStream();
            } catch (IOException e) {
                throw new LumifyClientApiException("Could not get input stream", e);
            }
        }
    }
}
