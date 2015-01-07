package io.lumify.analystsNotebook.aggregateClassification;

import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.VisibilityJson;
import org.securegraph.Vertex;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Set;

public class AggregateClassificationClient {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AggregateClassificationClient.class);
    private AggregateClassificationConfiguration aggregateClassificationConfiguration;

    public AggregateClassificationClient(Configuration configuration) {
        aggregateClassificationConfiguration = new AggregateClassificationConfiguration();
        configuration.setConfigurables(aggregateClassificationConfiguration, AggregateClassificationConfiguration.CONFIGURATION_PREFIX);
    }

    public String getClassificationBanner(Iterable<Vertex> vertices) {
        if (!aggregateClassificationConfiguration.isServiceConfigured()) {
            return null;
        }

        String[] visibilitySources = getUniqueVisibilitySources(vertices);

        try {
            URL url = getURL(visibilitySources);
            LOGGER.debug("aggregate classification request url is: %s", url);

            HttpURLConnection httpConnection;
            if (url.getProtocol().equalsIgnoreCase("https")) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();
                if (aggregateClassificationConfiguration.isTrustStoreConfigured()) {
                    LOGGER.debug("configuring SSLSocketFactory with custom TrustManager for https connection");
                    httpsConnection.setSSLSocketFactory(getSSLSocketFactory());
                }
                if (aggregateClassificationConfiguration.isDisableHostnameVerification()) {
                    LOGGER.debug("disabling host name verification for https connection");
                    httpsConnection.setHostnameVerifier(getHostnameVerifier());
                }
                httpsConnection.connect();
                httpConnection = httpsConnection;
            } else {
                httpConnection = (HttpURLConnection) url.openConnection();
                httpConnection.connect();
            }

            int responseCode = httpConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String responseMessage = httpConnection.getResponseMessage();
                throw new LumifyException(responseCode + " (" + responseMessage + ") while accessing: " + aggregateClassificationConfiguration.getServiceUrl());
            }

            String content = (String) httpConnection.getContent();
            LOGGER.debug("aggregate classification response content is: %s", content);
            return content;
        } catch (Exception e) {
            throw new LumifyException("exception while making the aggregate classification request", e);
        }
    }

    private String[] getUniqueVisibilitySources(Iterable<Vertex> vertices) {
        Set<String> visibilitySourceSet = new HashSet<String>();
        for (Vertex vertex : vertices) {
            VisibilityJson visibilityJson = LumifyProperties.VISIBILITY_JSON.getPropertyValue(vertex);
            if (visibilityJson != null) {
                String visibilitySource = visibilityJson.getSource();
                if (visibilitySource != null && visibilitySource.trim().length() > 0) {
                    visibilitySourceSet.add(visibilitySource);
                }
            }
        }
        return visibilitySourceSet.toArray(new String[visibilitySourceSet.size()]);
    }

    private URL getURL(String[] visibilitySources) throws MalformedURLException {
        String serviceUrl = aggregateClassificationConfiguration.getServiceUrl();
        String parameterName = aggregateClassificationConfiguration.getParameterName();
        StringBuilder sb = new StringBuilder();
        sb.append(serviceUrl);
        for (int i = 0; i < visibilitySources.length; i++) {
            sb.append(i == 0 ? "?" : "&").append(parameterName).append("=").append(visibilitySources[i]);
        }
        return new URL(sb.toString());
    }

    private SSLSocketFactory getSSLSocketFactory() throws GeneralSecurityException, IOException {
        KeyManager[] keyManagers = new KeyManager[]{};
        TrustManager[] trustManagers = getTrustManagers();
        SSLContext sslContext = SSLContext.getInstance("TLSv1");
        sslContext.init(keyManagers, trustManagers, null);
        return sslContext.getSocketFactory();
    }

    private TrustManager[] getTrustManagers() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        File trustStoreFile = new File(aggregateClassificationConfiguration.getTrustStorePath());
        FileInputStream trustStoreFileInputStream = new FileInputStream(trustStoreFile);
        KeyStore trustStore = KeyStore.getInstance("JKS");  // TODO: choose the type by file extension
        char[] trustStorePassword = aggregateClassificationConfiguration.getTrustStorePassword().toCharArray();
        trustStore.load(trustStoreFileInputStream, trustStorePassword);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return trustManagerFactory.getTrustManagers();
    }

    private HostnameVerifier getHostnameVerifier() {
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
        return hostnameVerifier;
    }
}
