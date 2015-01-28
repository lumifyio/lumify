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
import java.util.Locale;

public class FormulaEvaluator {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FormulaEvaluator.class);
    private Configuration configuration;
    private OntologyRepository ontologyRepository;
    private Locale locale;
    private String timeZone;
    private static final ThreadLocal<ScriptableObject> threadLocalScope = new ThreadLocal<>();

    @Inject
    public FormulaEvaluator(Configuration configuration, OntologyRepository ontologyRepository, Locale locale, String timeZone) {
        this.configuration = configuration;
        this.ontologyRepository = ontologyRepository;
        this.locale = locale == null ? Locale.getDefault() : locale;
        this.timeZone = timeZone;
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

    public String evaluateTitleFormula(Vertex vertex, String workspaceId, Authorizations authorizations) {
        return evaluateFormula("Title", vertex, workspaceId, authorizations);
    }

    public String evaluateTimeFormula(Vertex vertex, String workspaceId, Authorizations authorizations) {
        return evaluateFormula("Time", vertex, workspaceId, authorizations);
    }

    public String evaluateSubtitleFormula(Vertex vertex, String workspaceId, Authorizations authorizations) {
        return evaluateFormula("Subtitle", vertex, workspaceId, authorizations);
    }

    private String evaluateFormula(String type, Vertex vertex, String workspaceId, Authorizations authorizations) {
        if (Context.getCurrentContext() == null || threadLocalScope.get() == null) {
            initializeEnvironment();
        }

        Scriptable scope = threadLocalScope.get();

        String json = toJson(vertex, workspaceId, authorizations);
        Function function = (Function) scope.get("evaluate" + type + "FormulaJson", scope);
        Object result = function.call(Context.getCurrentContext(), scope, scope, new Object[]{json});

        return (String) Context.jsToJava(result, String.class);
    }

    protected void initializeEnvironment() {
        synchronized (threadLocalScope) {
            ScriptableObject scope = setupContext(getOntologyJson(), getConfigurationJson(), timeZone);
            threadLocalScope.set(scope);
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

    protected String getConfigurationJson() {
        return configuration.toJSON(this.locale).toString();
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
}
