package io.lumify.core.util;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class FixedSizeCircularLinkedListTest {

    @Test
    public void setupAndToString() {
        int size = 5;
        FixedSizeCircularLinkedList<AtomicInteger> list = new FixedSizeCircularLinkedList<AtomicInteger>(size, AtomicInteger.class);
        assertEquals("0:0,1:0,2:0,3:0,4:0", list.toString());
    }

    @Test
    public void set() {
        int size = 5;
        FixedSizeCircularLinkedList<AtomicInteger> list = new FixedSizeCircularLinkedList<AtomicInteger>(size, AtomicInteger.class);
        for (int i = 0; i < size; i++) {
            list.head().set(i);
            list.rotateForward();
        }
        for (int i = 0; i < size; i++) {
            assertEquals(i, list.head().get());
            list.rotateForward();
        }
    }

    @Test
    public void increment() {
        int size = 5;
        FixedSizeCircularLinkedList<AtomicInteger> list = new FixedSizeCircularLinkedList<AtomicInteger>(size, AtomicInteger.class);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < i; j++) {
                list.head().incrementAndGet();
            }
            list.rotateForward();
        }
        for (int i = 0; i < size; i++) {
            assertEquals(i, list.head().get());
            list.rotateForward();
        }
    }

    @Test
    public void circularity() {
        int size = 5;
        FixedSizeCircularLinkedList<AtomicInteger> list = new FixedSizeCircularLinkedList<AtomicInteger>(size, AtomicInteger.class);
        for (int i = 0; i < size; i++) {
            list.head().set(i);
            list.rotateForward();
        }
        for (int i = 0; i < size * 2; i++) {
            assertEquals(i % size, list.head().get());
            list.rotateForward();
        }
    }

    @Test
    public void readBackward() {
        int size = 16;
        FixedSizeCircularLinkedList<AtomicInteger> list = new FixedSizeCircularLinkedList<AtomicInteger>(size, AtomicInteger.class);
        for (int i = 0; i < size; i++) {
            list.head().set(i);
            list.rotateForward();
        }

        // one
        assertEquals(15, list.readBackward(1).get(0).get());

        // five
        List<AtomicInteger> five = list.readBackward(5);
        int sumOfFive = 0;
        for (int i = 0; i < 5; i++) {
            sumOfFive += five.get(i).get();
        }
        assertEquals(65, sumOfFive);

        // fifteen
        List<AtomicInteger> fifteen = list.readBackward(15);
        int sumOfFifteen = 0;
        for (int i = 0; i < 15; i++) {
            sumOfFifteen += fifteen.get(i).get();
        }
        assertEquals(120, sumOfFifteen);
    }
}
