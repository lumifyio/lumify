package io.lumify.core.formula;

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

/**
 * Not Thread Safe, Do not share instance across threads
 */
public class FormulaEvaluator {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FormulaEvaluator.class);
    private Configuration configuration;
    private OntologyRepository ontologyRepository;
    private Context context;
    private ScriptableObject scope;
    private Locale locale;
    private String timeZone;
    private boolean skipCloseWarning;

    @Inject
    public FormulaEvaluator(Configuration configuration, OntologyRepository ontologyRepository, Locale locale, String timeZone) {
        this.configuration = configuration;
        this.ontologyRepository = ontologyRepository;
        this.locale = locale == null ? Locale.getDefault() : locale;
        this.timeZone = timeZone;
        this.skipCloseWarning = false;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!skipCloseWarning) {
            LOGGER.warn("close() method not called to clean up JavaScript Context");
            this.close();
        }
    }

    public void close() {
        if (context != null) {
            context.exit();
        }
        this.skipCloseWarning = true;
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
        if (context == null) {
            initializeEnvironment();
        }

        String json = toJson(vertex, workspaceId, authorizations);
        Function function = (Function) scope.get("evaluate" + type + "FormulaJson", scope);
        Object result = function.call(context, scope, scope, new Object[]{json});

        return (String) Context.jsToJava(result, String.class);
    }

    protected void initializeEnvironment() {
        setupContext();
        loadJavaScript();
    }

    protected void setupContext() {
        if (this.context != null) {
            Context.exit();
        }
        this.context = Context.enter();
        context.setLanguageVersion(Context.VERSION_1_6);

        final RequireJsSupport browserSupport = new RequireJsSupport();

        this.scope = (ScriptableObject) context.initStandardObjects(browserSupport);

        try {
            scope.put("ONTOLOGY_JSON", scope, Context.toObject(getOntologyJson(), scope));
            scope.put("CONFIG_JSON", scope, Context.toObject(getConfigurationJson(), scope));
            scope.put("USERS_TIMEZONE", scope, Context.toObject(timeZone, scope));
        } catch (Exception e) {
            throw new LumifyException("Json resource not available", e);
        }

        String[] names = new String[] { "print", "load", "consoleWarn", "consoleError", "readFile" };
        browserSupport.defineFunctionProperties(names, scope.getClass(), ScriptableObject.DONTENUM);

        Scriptable argsObj = context.newArray(scope, new Object[]{});
        scope.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);
    }

    private void loadJavaScript() {
        evaluateFile("libs/underscore.js");
        evaluateFile("libs/r.js");
        evaluateFile("libs/windowTimers.js");
        evaluateFile("loader.js");
    }

    protected String getOntologyJson() throws Exception {
        ClientApiOntology result = ontologyRepository.getClientApiObject();
        return ObjectMapperFactory.getInstance().writeValueAsString(result);
    }

    protected String getConfigurationJson() throws Exception {
        return configuration.toJSON(this.locale).toString();
    }

    private Object evaluateFile(String filename) {
        InputStream is = FormulaEvaluator.class.getResourceAsStream(filename);
        if (is != null) {
            try {
                return context.evaluateString(scope, IOUtils.toString(is), filename, 0, null);
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
