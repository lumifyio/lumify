package io.lumify.core.formula;

import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.configuration.MockAnnotationProcessor;
import org.securegraph.Vertex;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class FormulaEvaluatorTest {

    private static FormulaEvaluator evaluator;

    private static Configuration configuration;

    @Mock
    private static OntologyRepository ontologyRepository;
    private static Locale locale;
    private static String timeZone;

    @BeforeClass
    public static void setUp() throws Exception {
        locale = Locale.getDefault();
        timeZone = TimeZone.getDefault().getDisplayName();

        ontologyRepository = mock(OntologyRepository.class);

        Map<String, String> map = new HashMap<String, String>();
        ConfigurationLoader configurationLoader = new HashMapConfigurationLoader(map);
        configuration = configurationLoader.createConfiguration();

        evaluator = spy(new FormulaEvaluator(configuration, ontologyRepository, locale, timeZone));

        String ontologyJson = IOUtils.toString(FormulaEvaluatorTest.class.getResourceAsStream("ontology.json"), "utf-8");
        String configurationJson = IOUtils.toString(FormulaEvaluatorTest.class.getResourceAsStream("configuration.json"), "utf-8");
        String vertexJson = IOUtils.toString(FormulaEvaluatorTest.class.getResourceAsStream("vertex.json"), "utf-8");

        doReturn(ontologyJson).when(evaluator).getOntologyJson();
        doReturn(configurationJson).when(evaluator).getConfigurationJson();

        doReturn(vertexJson).when(evaluator).toJson(null, null, null);
    }

    @AfterClass
    public static void teardown() {
        evaluator.close();
    }

    @Test
    public void testEvaluatorJson() throws Exception {
        assertTrue(evaluator.getOntologyJson().length() > 0);
        assertTrue(evaluator.getConfigurationJson().length() > 0);
    }

    @Test
    public void testEvaluateTitleFormula() {
        assertEquals("Prop A Value, Prop B Value", evaluator.evaluateTitleFormula(null, null, null));
    }

    @Test
    public void testEvaluateSubtitleFormula() {
        assertEquals("Prop C Value", evaluator.evaluateSubtitleFormula(null, null, null));
    }

    @Test
    public void testEvaluateTimeFormula() {
        assertEquals("2014-11-20", evaluator.evaluateTimeFormula(null, null, null));
    }
}