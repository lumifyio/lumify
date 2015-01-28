package io.lumify.core.formula;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.util.ClientApiConverter;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.ClientApiOntology;
import io.lumify.web.clientapi.model.ClientApiVertex;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class FormulaEvaluator {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FormulaEvaluator.class);
    private Configuration configuration;
    private OntologyRepository ontologyRepository;
    private static final ThreadLocal<Map<String, ScriptableObject>> threadLocalScope = new ThreadLocal<>();

    @Inject
    public FormulaEvaluator(Configuration configuration, OntologyRepository ontologyRepository) {
        this.configuration = configuration;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (Context.getCurrentContext() != null) {
            LOGGER.warn("close() method not called to clean up JavaScript Context");
        }
    }

    public void close() {
        synchronized (threadLocalScope) {
            if (Context.getCurrentContext() != null) {
                Context.exit();
                threadLocalScope.remove();
            }
        }
    }

    public String evaluateTitleFormula(Vertex vertex, UserContext userContext, Authorizations authorizations) {
        return evaluateFormula("Title", vertex, userContext, authorizations);
    }

    public String evaluateTimeFormula(Vertex vertex, UserContext userContext, Authorizations authorizations) {
        return evaluateFormula("Time", vertex, userContext, authorizations);
    }

    public String evaluateSubtitleFormula(Vertex vertex, UserContext userContext, Authorizations authorizations) {
        return evaluateFormula("Subtitle", vertex, userContext, authorizations);
    }

    private String evaluateFormula(String type, Vertex vertex, UserContext userContext, Authorizations authorizations) {
        checkNotNull(userContext, "userContext cannot be null");
        Scriptable scope = getScriptable(userContext.getLocale(), userContext.getTimeZone());

        String json = toJson(vertex, userContext.getWorkspaceId(), authorizations);
        Function function = (Function) scope.get("evaluate" + type + "FormulaJson", scope);
        Object result = function.call(Context.getCurrentContext(), scope, scope, new Object[]{json});

        return (String) Context.jsToJava(result, String.class);
    }

    protected Scriptable getScriptable(Locale locale, String timeZone) {
        synchronized (threadLocalScope) {
            Map<String, ScriptableObject> map = threadLocalScope.get();
            if (map == null) {
                map = new HashMap<>();
                threadLocalScope.set(map);
            }
            String mapKey = locale.toString() + timeZone;
            ScriptableObject scope = map.get(mapKey);
            if (scope == null) {
                scope = setupContext(getOntologyJson(), getConfigurationJson(locale), timeZone);
                map.put(mapKey, scope);
            }
            return scope;
        }
    }

    protected static ScriptableObject setupContext(String ontologyJson, String configurationJson, String timeZone) {
        if (Context.getCurrentContext() != null) {
            Context.exit();
            threadLocalScope.remove();
        }
        Context context = Context.enter();
        context.setLanguageVersion(Context.VERSION_1_6);

        final RequireJsSupport browserSupport = new RequireJsSupport();

        ScriptableObject scope = context.initStandardObjects(browserSupport, true);

        try {
            scope.put("ONTOLOGY_JSON", scope, Context.toObject(ontologyJson, scope));
            scope.put("CONFIG_JSON", scope, Context.toObject(configurationJson, scope));
            scope.put("USERS_TIMEZONE", scope, Context.toObject(timeZone, scope));
        } catch (Exception e) {
            throw new LumifyException("Json resource not available", e);
        }

        String[] names = new String[]{"print", "load", "consoleWarn", "consoleError", "readFile"};
        browserSupport.defineFunctionProperties(names, scope.getClass(), ScriptableObject.DONTENUM);

        Scriptable argsObj = context.newArray(scope, new Object[]{});
        scope.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);

        loadJavaScript(scope);

        scope.sealObject();
        return scope;
    }

    private static void loadJavaScript(ScriptableObject scope) {
        evaluateFile(scope, "libs/underscore.js");
        evaluateFile(scope, "libs/r.js");
        evaluateFile(scope, "libs/windowTimers.js");
        evaluateFile(scope, "loader.js");
    }

    protected String getOntologyJson() {
        ClientApiOntology result = ontologyRepository.getClientApiObject();
        try {
            return ObjectMapperFactory.getInstance().writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new LumifyException("Could not evaluate JSON: " + result, ex);
        }
    }

    protected String getConfigurationJson(Locale locale) {
        return configuration.toJSON(locale).toString();
    }

    private static Object evaluateFile(ScriptableObject scope, String filename) {
        InputStream is = FormulaEvaluator.class.getResourceAsStream(filename);
        if (is != null) {
            try {
                return Context.getCurrentContext().evaluateString(scope, IOUtils.toString(is), filename, 0, null);
            } catch (IOException e) {
                LOGGER.error("File not readable %s", filename);
            }
        } else LOGGER.error("File not found %s", filename);
        return null;
    }

    protected String toJson(Vertex vertex, String workspaceId, Authorizations authorizations) {
        ClientApiVertex v = ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations);
        return v.toString();
    }

    public static class UserContext {
        private final Locale locale;
        private final String timeZone;
        private final String workspaceId;

        public UserContext(Locale locale, String timeZone, String workspaceId) {
            this.locale = locale == null ? Locale.getDefault() : locale;
            this.timeZone = timeZone;
            this.workspaceId = workspaceId;
        }

        public Locale getLocale() {
            return locale;
        }

        public String getTimeZone() {
            return timeZone;
        }

        public String getWorkspaceId() {
            return workspaceId;
        }
    }
}
