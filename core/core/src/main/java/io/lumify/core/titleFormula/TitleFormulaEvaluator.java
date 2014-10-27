package io.lumify.core.titleFormula;

import com.google.inject.Inject;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.systemNotification.SystemNotification;
import io.lumify.core.util.JavaScriptEngine;
import org.apache.commons.io.IOUtils;
import org.securegraph.Vertex;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class TitleFormulaEvaluator {
    private static final String ENGINE_NAME = "JavaScript";
    private OntologyRepository ontologyRepository;
    ScriptEngine scriptEngine;

    private static final Map<String, String> fileMap;
    static {
        fileMap = new HashMap<String, String>();
        fileMap.put("define.js", "define.js");
        fileMap.put("service/config", "service_config.js");
        fileMap.put("service/ontology", "service_ontology.js");
        fileMap.put("service/serviceBase", "service_serviceBase.js");
        fileMap.put("util/formatters", "util_formatters.js");
        fileMap.put("util/messages", "util_messages.js");
        fileMap.put("promise!./service/messagesPromise", "util_service_messagesPromise.js");
        fileMap.put("promise!../service/ontologyPromise", "util_service_ontologyPromise.js");
        fileMap.put("formatters.js", "util_vertex_formatters.js");
        fileMap.put("./formula", "util_vertex_formula.js");
        fileMap.put("./urlFormatters", "util_vertex_urlFormatters.js");
    }

    @Inject
    public TitleFormulaEvaluator(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;

        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        scriptEngine = scriptEngineManager.getEngineByName(ENGINE_NAME);

        try {
            scriptEngine.put("TitleFormulaEvaluator", this);
            scriptEngine.eval(getJavaScriptFile("define.js"));
        } catch (ScriptException e) {
            throw new LumifyException("", e);
        } catch (IOException e) {
            throw new LumifyException("", e);
        }
    }

    public Object evaluate(String string) {
        try {
            return scriptEngine.eval(string);
        } catch (ScriptException e) {
            throw new LumifyException("", e);
        }
    }

    public String evaluate(Vertex vertex) {
        String conceptType = LumifyProperties.CONCEPT_TYPE.getPropertyValue(vertex);
        Concept concept = ontologyRepository.getConceptByIRI(conceptType);
        String titleFormula = concept.getTitleFormula();

        // TODO: magic here

        return titleFormula;
    }

    public String getJavaScriptFile(String filename) throws IOException {
        if (fileMap.containsKey(filename)) {
            InputStream is = TitleFormulaEvaluator.class.getResourceAsStream(fileMap.get(filename));
            return IOUtils.toString(is);
        } else {
            throw new LumifyException("unexpected request for JavaScript resource: " + filename);
        }
    }

    public static void main(String[] args) throws ScriptException, NoSuchMethodException, IOException {
        TitleFormulaEvaluator titleFormulaEvaluator = new TitleFormulaEvaluator(null);
        Object o = titleFormulaEvaluator.evaluate("define(['service/config', 'service/ontology'], function(a, b) {return a + b;});");
        System.out.println(o);
    }
}
