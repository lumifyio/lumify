package io.lumify.core.titleFormula;

import com.google.inject.Inject;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.web.clientapi.model.ClientApiOntology;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;
import org.apache.commons.io.IOUtils;
import org.securegraph.Vertex;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
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
        fileMap.put("timezone-js", "timezone.js");
        fileMap.put("sf", "sf.js");
        fileMap.put("underscore", "underscore.js");
        fileMap.put("service/config", "service_config.js");
        fileMap.put("service/ontology", "service_ontology.js");
        fileMap.put("service/serviceBase", "serviceBase.js");
        fileMap.put("util/formatters", "util_formatters.js");
        fileMap.put("util/messages", "messages.js");
        fileMap.put("promise!./service/messagesPromise", "util_service_messagesPromise.js");
        fileMap.put("promise!../service/ontologyPromise", "util_service_ontologyPromise.js");
        fileMap.put("util/vertex/formatters", "util_vertex_formatters.js");
        fileMap.put("./formula", "util_vertex_formula.js");
        fileMap.put("./urlFormatters", "util_vertex_urlFormatters.js");

        fileMap.put("testing.js", "testing.js");
    }

    @Inject
    public TitleFormulaEvaluator(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;

        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        scriptEngine = scriptEngineManager.getEngineByName(ENGINE_NAME);

        try {
            scriptEngine.put("TitleFormulaEvaluator", this);
            //scriptEngine.eval("window = this");
            //scriptEngine.eval(getJavaScriptFile("jquery.js"));
            //scriptEngine.eval(getJavaScriptFile("underscore"));
            scriptEngine.eval(getJavaScriptFile("sf"));
            //scriptEngine.eval(getJavaScriptFile("define.js"));
            //System.out.println(scriptEngine.eval(getJavaScriptFile("testing.js")));
        } catch (ScriptException e) {
            throw new LumifyException("", e);
        } catch (IOException e) {
            throw new LumifyException("", e);
        }
    }

    public Object evaluate(String string, String path) throws Exception {
        //try {
            return scriptEngine.eval(string);
        //} catch (ScriptException e) {
         //   throw new LumifyException(path, e);
        //}
    }

    public String evaluate(Vertex vertex) {
        String conceptType = LumifyProperties.CONCEPT_TYPE.getPropertyValue(vertex);
        Concept concept = ontologyRepository.getConceptByIRI(conceptType);
        String titleFormula = concept.getTitleFormula();

        // TODO: magic here

        return titleFormula;
    }

    public String getOntologyJson() throws Exception {
        if (ontologyRepository == null) {
            return IOUtils.toString(TitleFormulaEvaluator.class.getResourceAsStream("ontology.json"));
        } else {
            ClientApiOntology result = ontologyRepository.getClientApiObject();
            return ObjectMapperFactory.getInstance().writeValueAsString(result);
        }
    }

    public String getJavaScriptFile(String filename) throws IOException {
        if (fileMap.containsKey(filename)) {
            InputStream is = TitleFormulaEvaluator.class.getResourceAsStream(fileMap.get(filename));
            return IOUtils.toString(is);
        }
        return null;
    }

    public static void main(String[] args) throws ScriptException, NoSuchMethodException, IOException {
        TitleFormulaEvaluator titleFormulaEvaluator = new TitleFormulaEvaluator(null);
        //Object o = titleFormulaEvaluator.evaluate("define(['service/config', 'service/ontology'], function(a, b) {return a + b;});");
        //System.out.println(o);
    }
}
