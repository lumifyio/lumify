package com.altamiracorp.lumify.core.util;

import com.altamiracorp.lumify.core.metrics.JmxMetricsManager;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ThreadedInputStreamProcessTest {
    @Mock
    private JmxMetricsManager metricsManager;

    @Mock
    private Counter metricCounter;

    @Mock
    private Timer metricTimer;

    @Mock
    private Timer.Context metricTimerContext;

    @Before
    public void before() {
        when(metricsManager.getNamePrefix(anyObject())).thenReturn("metric1");
        when(metricsManager.counter(anyString())).thenReturn(metricCounter);
        when(metricsManager.timer(anyString())).thenReturn(metricTimer);
        when(metricTimer.time()).thenReturn(metricTimerContext);
    }

    @Test
    public void testDoWork() throws Exception {
        ArrayList<ThreadedTeeInputStreamWorker<byte[], String>> workers = new ArrayList<ThreadedTeeInputStreamWorker<byte[], String>>();
        TestThreadedTeeInputStreamWorker worker1 = new TestThreadedTeeInputStreamWorker("1");
        worker1.setMetricsManager(metricsManager);
        workers.add(worker1);
        TestThreadedTeeInputStreamWorker worker2 = new TestThreadedTeeInputStreamWorker("2");
        worker2.setMetricsManager(metricsManager);
        workers.add(worker2);
        ThreadedInputStreamProcess process = new ThreadedInputStreamProcess<byte[], String>("test", workers);

        byte[] data = createMockData(10);

        // first run
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        List<ThreadedTeeInputStreamWorker.WorkResult<byte[]>> results = process.doWork(in, "test1");
        assertEquals(2, results.size());

        assertEquals(null, results.get(0).getError());
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write("1".getBytes());
        expected.write("test1".getBytes());
        expected.write(data);
        assertArrayEquals(expected.toByteArray(), results.get(0).getResult());

        assertEquals(null, results.get(1).getError());
        expected = new ByteArrayOutputStream();
        expected.write("2".getBytes());
        expected.write("test1".getBytes());
        expected.write(data);
        assertArrayEquals(expected.toByteArray(), results.get(1).getResult());

        // second run
        in = new ByteArrayInputStream(data);
        results = process.doWork(in, "test2");
        assertEquals(2, results.size());

        assertEquals(null, results.get(0).getError());
        expected = new ByteArrayOutputStream();
        expected.write("1".getBytes());
        expected.write("test2".getBytes());
        expected.write(data);
        assertArrayEquals(expected.toByteArray(), results.get(0).getResult());

        assertEquals(null, results.get(1).getError());
        expected = new ByteArrayOutputStream();
        expected.write("2".getBytes());
        expected.write("test2".getBytes());
        expected.write(data);
        assertArrayEquals(expected.toByteArray(), results.get(1).getResult());
    }

    @Test
    public void testDoWorkWithException() throws Exception {
        ArrayList<ThreadedTeeInputStreamWorker<byte[], String>> workers = new ArrayList<ThreadedTeeInputStreamWorker<byte[], String>>();
        TestThreadedTeeInputStreamWorker worker1 = new TestThreadedTeeInputStreamWorker("1");
        worker1.setMetricsManager(metricsManager);
        workers.add(worker1);
        TestThreadedTeeInputStreamWorkerWithException worker2 = new TestThreadedTeeInputStreamWorkerWithException("2");
        worker2.setMetricsManager(metricsManager);
        workers.add(worker2);
        ThreadedInputStreamProcess process = new ThreadedInputStreamProcess<byte[], String>("test", workers);

        byte[] data = createMockData(10);

        // first run
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        List<ThreadedTeeInputStreamWorker.WorkResult<byte[]>> results = process.doWork(in, "test1", 5);
        assertEquals(2, results.size());

        assertEquals(null, results.get(0).getError());
        assertNotEquals(null, results.get(1).getError());
        assertEquals("2 throwing exception", results.get(1).getError().getMessage());

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write("1".getBytes());
        expected.write("test1".getBytes());
        expected.write(data);
        assertArrayEquals(expected.toByteArray(), results.get(0).getResult());
    }

    private byte[] createMockData(int len) {
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            data[i] = (byte) i;
        }
        return data;
    }

    private static class TestThreadedTeeInputStreamWorker extends ThreadedTeeInputStreamWorker<byte[], String> {
        private final String prefix;

        public TestThreadedTeeInputStreamWorker(String prefix) {
            this.prefix = prefix;
        }

        @Override
        protected byte[] doWork(InputStream work, String s) throws Exception {
            Thread.sleep(new Random().nextInt(100));
            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            temp.write(prefix.getBytes());
            temp.write(s.getBytes());
            IOUtils.copy(work, temp);
            return temp.toByteArray();
        }
    }

    private static class TestThreadedTeeInputStreamWorkerWithException extends ThreadedTeeInputStreamWorker<byte[], String> {
        private final String prefix;

        public TestThreadedTeeInputStreamWorkerWithException(String prefix) {
            this.prefix = prefix;
        }

        @Override
        protected byte[] doWork(InputStream work, String s) throws Exception {
            throw new RuntimeException(prefix + " throwing exception");
        }
    }
}
