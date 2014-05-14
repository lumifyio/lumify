package io.lumify.http;

import java.util.ArrayList;
import java.util.List;

public class HttpRequestParameterBuilder {
    private List<NameValuePair> params = new ArrayList<NameValuePair>();

    public void addParameter(String name, String value) {
        addParameter(new NameValuePair(name, value));
    }

    public void addParameter(NameValuePair parameter) {
        this.params.add(parameter);
    }

    public String build() {
        StringBuilder requestParameterString = new StringBuilder();
        boolean isFirstParameter = true;
        for (NameValuePair pair : params) {
            if (isFirstParameter) {
                isFirstParameter = false;
            } else {
                requestParameterString.append("&");
            }

            requestParameterString.append(pair.getUrlEncodedName())
                    .append("=")
                    .append(pair.getUrlEncodedValue());
        }

        return requestParameterString.toString();
    }
}
