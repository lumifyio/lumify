package io.lumify.core.config;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ConfigurationTest {
    private static Configuration configuration;

    @BeforeClass
    public static void setUp() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("rabbitmq.addr.0.host", "10.0.1.101");
        map.put("rabbitmq.addr.2.host", "10.0.1.103");
        map.put("rabbitmq.addr.1.host", "10.0.1.102");
        map.put("foo", "A");
        map.put("bar", "B");
        map.put("bar.baz", "C");

        ConfigurationLoader configurationLoader = new HashMapConfigurationLoader(map);
        configuration = configurationLoader.createConfiguration();
    }

    @Test
    public void testGetSubset() {
        Map<String, String> subset = configuration.getSubset("rabbitmq.addr");
        assertEquals(3, subset.size());
        assertTrue(subset.keySet().contains("0.host"));
        assertTrue(subset.keySet().contains("1.host"));
        assertTrue(subset.keySet().contains("2.host"));
        assertEquals("10.0.1.101", subset.get("0.host"));
        assertEquals("10.0.1.102", subset.get("1.host"));
        assertEquals("10.0.1.103", subset.get("2.host"));
    }

    @Test
    public void testGetKeysWithPrefix() {
        Set<String> addrKeys = (Set<String>) configuration.getKeys("rabbitmq.addr.");
        assertEquals(3, addrKeys.size());
        assertTrue(addrKeys.contains("rabbitmq.addr.0.host"));
        assertTrue(addrKeys.contains("rabbitmq.addr.1.host"));
        assertTrue(addrKeys.contains("rabbitmq.addr.2.host"));

        Set<String> barKeys = (Set<String>) configuration.getKeys("bar");
        assertEquals(2, barKeys.size());
        assertTrue(barKeys.contains("bar"));
        assertTrue(barKeys.contains("bar.baz"));

        Set<String> barDotKeys = (Set<String>) configuration.getKeys("bar.");
        assertEquals(1, barDotKeys.size());
        assertTrue(barDotKeys.contains("bar.baz"));
    }

    @Test
    public void testGet() {
        String hit = configuration.get("foo", null);
        assertEquals("A", hit);

        String miss = configuration.get("no.such.key", null);
        assertEquals(null, miss);
    }
}
