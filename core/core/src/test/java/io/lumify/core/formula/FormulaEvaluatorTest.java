package io.lumify.core.formula;

import io.lumify.core.config.Configuration;
import io.lumify.core.config.ConfigurationLoader;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.model.ontology.OntologyRepository;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class FormulaEvaluatorTest {

    private static FormulaEvaluator evaluator;

    @Mock
    private static OntologyRepository ontologyRepository;

    @BeforeClass
    public static void setUp() throws Exception {
        Locale locale = Locale.getDefault();
        String timeZone = TimeZone.getDefault().getDisplayName();

        Map<String, String> map = new HashMap<String, String>();
        ConfigurationLoader configurationLoader = new HashMapConfigurationLoader(map);
        Configuration configuration = configurationLoader.createConfiguration();

        final String ontologyJson = IOUtils.toString(FormulaEvaluatorTest.class.getResourceAsStream("ontology.json"), "utf-8");
        final String configurationJson = IOUtils.toString(FormulaEvaluatorTest.class.getResourceAsStream("configuration.json"), "utf-8");
        final String vertexJson = IOUtils.toString(FormulaEvaluatorTest.class.getResourceAsStream("vertex.json"), "utf-8");

        evaluator = new FormulaEvaluator(configuration, ontologyRepository, locale, timeZone) {
            @Override
            protected String getOntologyJson() {
                return ontologyJson;
            }

            @Override
            protected String getConfigurationJson() {
                return configurationJson;
            }

            @Override
            protected String toJson(Vertex vertex, String workspaceId, Authorizations authorizations) {
                return vertexJson;
            }
        };
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

    @Test
    public void testThreading() throws InterruptedException {
        Thread[] threads = new Thread[4];
        final AtomicInteger threadsReadyCount = new AtomicInteger();
        final Semaphore block = new Semaphore(threads.length);
        block.acquire(threads.length);

        // prime the main thread for evaluation
        assertEquals("Prop A Value, Prop B Value", evaluator.evaluateTitleFormula(null, null, null));

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // prime this thread for evaluation
                        evaluator.evaluateTitleFormula(null, null, null);
                        threadsReadyCount.incrementAndGet();
                        block.acquire(); // wait to run the look
                        for (int i = 0; i < 20; i++) {
                            System.out.println(Thread.currentThread().getName() + " - " + i);
                            assertEquals("Prop A Value, Prop B Value", evaluator.evaluateTitleFormula(null, null, null));
                        }
                        System.out.println(Thread.currentThread().getName() + " - closing evaluator");
                        evaluator.close();
                        System.out.println(Thread.currentThread().getName() + " - evaluator closed");
                    } catch (Exception ex) {
                        throw new RuntimeException("Could not run", ex);
                    }
                }
            });
            threads[i].setName(FormulaEvaluatorTest.class.getSimpleName() + "-testThreading-" + i);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        // wait for all threads to be primed
        while (threadsReadyCount.get() < threads.length) {
            Thread.sleep(100);
        }
        block.release(threads.length);

        // wait for threads to finish
        for (Thread thread : threads) {
            synchronized (thread) {
                thread.join();
            }
        }

        // make sure the main threads evaluator isn't broken.
        assertEquals("Prop A Value, Prop B Value", evaluator.evaluateTitleFormula(null, null, null));
        evaluator.close();
    }
}