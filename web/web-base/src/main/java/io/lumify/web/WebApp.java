package io.lumify.web;

import com.google.inject.Injector;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.LumifyResourceBundleManager;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.App;
import io.lumify.miniweb.Handler;
import io.lumify.miniweb.handlers.AppendableStaticResourceHandler;
import io.lumify.miniweb.handlers.StaticResourceHandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.securegraph.util.CloseableUtils.closeQuietly;

public class WebApp extends App {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WebApp.class);
    private final Injector injector;
    private final boolean devMode;
    private final AppendableStaticResourceHandler pluginsJsResourceHandler = new No404AppendableStaticResourceHandler("application/javascript");
    private final List<String> pluginsJsResources = new ArrayList<String>();
    private final AppendableStaticResourceHandler pluginsCssResourceHandler = new No404AppendableStaticResourceHandler("text/css");
    private final List<String> pluginsCssResources = new ArrayList<String>();
    private LumifyResourceBundleManager lumifyResourceBundleManager = new LumifyResourceBundleManager();

    public WebApp(final ServletContext servletContext, final Injector injector) {
        super(servletContext);
        this.injector = injector;

        Configuration config = injector.getInstance(Configuration.class);
        this.devMode = "true".equals(config.get(Configuration.DEV_MODE, "false"));

        if (!devMode) {
            String pluginsJsRoute = "plugins.js";
            this.get("/" + pluginsJsRoute, pluginsJsResourceHandler);
            pluginsJsResources.add(pluginsJsRoute);

            String pluginsCssRoute = "plugins.css";
            this.get("/" + pluginsCssRoute, pluginsCssResourceHandler);
            pluginsCssResources.add(pluginsCssRoute);
        }
    }

    @Override
    protected Handler[] instantiateHandlers(Class<? extends Handler>[] handlerClasses) throws Exception {
        Handler[] handlers = new Handler[handlerClasses.length];
        for (int i = 0; i < handlerClasses.length; i++) {
            handlers[i] = injector.getInstance(handlerClasses[i]);
        }
        return handlers;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (request.getRequestURI().endsWith("ejs")) {
            response.setContentType("text/plain");
        }
        response.setCharacterEncoding("UTF-8");
        super.handle(request, response);
    }

    public void registerJavaScript(String scriptResourceName) {
        String resourcePath = "js" + scriptResourceName;
        if (devMode) {
            get("/" + resourcePath, new StaticResourceHandler(this.getClass(), scriptResourceName, "application/javascript"));
            pluginsJsResources.add(resourcePath);
        } else {
            pluginsJsResourceHandler.appendResource(scriptResourceName);
        }
    }

    public void registerCss(String cssResourceName) {
        String resourcePath = "css" + cssResourceName;
        if (devMode) {
            get("/" + resourcePath, new StaticResourceHandler(this.getClass(), cssResourceName, "text/css"));
            pluginsCssResources.add(resourcePath);
        } else {
            pluginsCssResourceHandler.appendResource(cssResourceName);
        }
    }

    public static Locale getLocal(String language, String country, String variant) {
        if (language != null) {
            if (country != null) {
                if (variant != null) {
                    return new Locale(language, country, variant);
                }
                return new Locale(language, country);
            }
            return new Locale(language);
        }
        return Locale.getDefault();
    }

    public void registerResourceBundle(String resourceBundleResourceName) {
        InputStream stream = WebApp.class.getResourceAsStream(resourceBundleResourceName);
        if (stream == null) {
            throw new LumifyException("Could not find resource bundle resource: " + resourceBundleResourceName);
        }
        try {
            Pattern pattern = Pattern.compile(".*_([a-z]{2})(?:_([A-Z]{2}))?(?:_(.+))?\\.properties");
            Matcher matcher = pattern.matcher(resourceBundleResourceName);
            if (matcher.matches()) {
                String language = matcher.group(1);
                String country = matcher.group(2);
                String variant = matcher.group(3);
                Locale locale = getLocal(language, country, variant);
                LOGGER.info("registering ResourceBundle plugin file: %s with locale: %s", resourceBundleResourceName, locale);
                lumifyResourceBundleManager.register(stream, locale);
            } else {
                LOGGER.info("registering ResourceBundle plugin file: %s", resourceBundleResourceName);
                lumifyResourceBundleManager.register(stream);
            }
        } catch (IOException e) {
            throw new LumifyException("Could not read resource bundle resource: " + resourceBundleResourceName);
        } finally {
            closeQuietly(stream);
        }
    }

    public ResourceBundle getBundle(Locale locale) {
        return lumifyResourceBundleManager.getBundle(locale);
    }

    public List<String> getPluginsJsResources() {
        return pluginsJsResources;
    }

    public List<String> getPluginsCssResources() {
        return pluginsCssResources;
    }

    public boolean isDevModeEnabled() {
        return devMode;
    }
}
