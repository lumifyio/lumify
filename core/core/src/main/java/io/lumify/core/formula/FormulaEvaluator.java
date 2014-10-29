package io.lumify.core.formula;

import com.google.inject.Inject;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.clientapi.model.ClientApiOntology;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.securegraph.Vertex;

import java.io.IOException;
import java.io.InputStream;

/**
 * Not Thread Safe, Do not share instance across threads
 */
public class FormulaEvaluator {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FormulaEvaluator.class);
    private OntologyRepository ontologyRepository;
    private Context context;
    private ScriptableObject scope;

    @Inject
    public FormulaEvaluator(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
        this.context = Context.enter();

        setupContext();
        loadJavaScript();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        LOGGER.warn("close() method not called to clean up JavaScript Context");
        this.close();
    }

    public void close() {
        context.exit();
    }

    public String evaluateTitleFormula(Vertex vertex) {
        return evaluateFormula("Title", vertex);
    }

    public String evaluateTimeFormula(Vertex vertex) {
        return evaluateFormula("Time", vertex);
    }

    public String evaluateSubtitleFormula(Vertex vertex) {
        return evaluateFormula("Subtitle", vertex);
    }

    private String evaluateFormula(String type, Vertex vertex) {
        String json = toJson(vertex);

        Function f = (Function) scope.get("evaluate" + type + "FormulaJson", scope);
        Object result = (String) f.call(context, scope, scope, new Object[]{json});

        return (String) Context.jsToJava(result, String.class);
    }

    private void setupContext() {
        context.setLanguageVersion(Context.VERSION_1_6);

        final RequireJsSupport browserSupport = new RequireJsSupport();

        this.scope = (ScriptableObject) context.initStandardObjects(browserSupport);

        try {
            scope.put("ONTOLOGY_JSON", scope, Context.toObject(getOntologyJson(), scope));
            scope.put("CONFIG_JSON", scope, Context.toObject(getConfigurationJson(), scope));
        } catch (Exception e) {
            throw new LumifyException("Json resource not available", e);
        }

        browserSupport.defineFunctionProperties(new String[] { "print", "load" }, scope.getClass(), ScriptableObject.DONTENUM);

        Scriptable argsObj = context.newArray(scope, new Object[] { });
        scope.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);
    }

    private void loadJavaScript() {
        evaluateFile("libs/underscore.js");
        evaluateFile("libs/r.js");
        evaluateFile("loader.js");
    }

    private String getOntologyJson() throws Exception {
        // TODO: use ontologyRepository
        if (ontologyRepository == null) {
            return IOUtils.toString(FormulaEvaluator.class.getResourceAsStream("mocks/ontology.json"));
        } else {
            ClientApiOntology result = ontologyRepository.getClientApiObject();
            return ObjectMapperFactory.getInstance().writeValueAsString(result);
        }
    }

    private String getConfigurationJson() throws Exception {
        // TODO: use real config json
        return IOUtils.toString(FormulaEvaluator.class.getResourceAsStream("mocks/configuration.json"));
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

    private String toJson(Vertex vertex) {
        // TODO: transform vertex
        return "{ \"properties\":[ " +
                "{\"name\":\"http://lumify.io/dev#birthDate\", \"value\": 1414617155146}," +
                "{\"name\":\"http://lumify.io#title\",         \"value\": \"Test Title for Person\"}," +
                "{\"name\":\"http://lumify.io#conceptType\",   \"value\": \"http://lumify.io/dev#person\"}" +
                "]}";
    }

    public static void main(String[] args) throws NoSuchMethodException, IOException {

        FormulaEvaluator formulaEvaluator = new FormulaEvaluator(null);
        System.out.println("Title: " + formulaEvaluator.evaluateTitleFormula(null));
        System.out.println("Subtitle: " + formulaEvaluator.evaluateSubtitleFormula(null));
        System.out.println("Time: " + formulaEvaluator.evaluateTimeFormula(null));
        formulaEvaluator.close();
    }

}
