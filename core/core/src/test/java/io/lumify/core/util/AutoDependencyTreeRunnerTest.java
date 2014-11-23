package io.lumify.core.util;

import com.google.common.base.Joiner;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AutoDependencyTreeRunnerTest {
    private List<String> foundOrder = new ArrayList<String>();
    private Runnable a = new FoundOrderRunnable(foundOrder, "a");
    private Runnable b = new FoundOrderRunnable(foundOrder, "b");
    private Runnable c = new FoundOrderRunnable(foundOrder, "c");
    private Runnable d = new FoundOrderRunnable(foundOrder, "d");
    private Runnable e = new FoundOrderRunnable(foundOrder, "e");

    @Test
    public void testInOrder() {
        foundOrder.clear();

        AutoDependencyTreeRunner tree = new AutoDependencyTreeRunner();
        tree.add(a, b, c);
        tree.add(c, d);
        tree.add(c, e);
        tree.run();

        assertEquals("a,b,c,d,e", Joiner.on(',').join(foundOrder));
    }

    @Test
    public void testOutOfOrder() {
        foundOrder.clear();

        AutoDependencyTreeRunner tree = new AutoDependencyTreeRunner();
        tree.add(c, d);
        tree.add(c, e);
        tree.add(a, b, c);
        tree.run();

        assertEquals("a,b,c,d,e", Joiner.on(',').join(foundOrder));
    }

    private static class FoundOrderRunnable implements Runnable {
        private final List<String> foundOrder;
        private final String name;

        public FoundOrderRunnable(List<String> foundOrder, String name) {
            this.foundOrder = foundOrder;
            this.name = name;
        }

        @Override
        public void run() {
            this.foundOrder.add(this.name);
        }
    }
}