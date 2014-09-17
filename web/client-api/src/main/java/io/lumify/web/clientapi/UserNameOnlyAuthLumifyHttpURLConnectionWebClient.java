package io.lumify.web.clientapi;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

public class UserNameOnlyAuthLumifyHttpURLConnectionWebClient extends LumifyHttpURLConnectionWebClient {
    private final String username;

    public UserNameOnlyAuthLumifyHttpURLConnectionWebClient(String baseUrl, String username) {
        super(baseUrl);
        this.username = username;
    }

    @Override
    protected String logIn() {
        try {
            URL url = createUrlFromUri("/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setDoOutput(true);

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes("username=" + URLEncoder.encode(this.username, "UTF-8"));
            out.flush();
            out.close();

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new LumifyClientApiException("Invalid response code. Expected 200. Found " + code);
            }
            Map<String, List<String>> responseHeaders = conn.getHeaderFields();
            List<String> cookies = responseHeaders.get("Set-Cookie");
            if (cookies == null) {
                throw new LumifyClientApiException("Could not find cookie header in response");
            }
            for (String cookie : cookies) {
                if (!cookie.startsWith("JSESSIONID=")) {
                    continue;
                }
                String cookieValue = cookie.substring("JSESSIONID=".length());
                int sep = cookieValue.indexOf(';');
                if (sep > 0) {
                    cookieValue = cookieValue.substring(0, sep);
                }
                return cookieValue;
            }
            throw new LumifyClientApiException("Could not find JSESSIONID cookie");
        } catch (Exception e) {
            throw new LumifyClientApiException("Could not login", e);
        }
    }
}
