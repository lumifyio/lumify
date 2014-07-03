package io.lumify.core.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TeeInputStreamTest {
    @Test
    public void testReads() throws Exception {
        byte[] temp = new byte[100];
        int readLen;
        byte[] data = createMockData(10);
        InputStream source = new ByteArrayInputStream(data);
        TeeInputStream in = new TeeInputStream(source, 2);
        in.loop(); // force a read
        in.loop(); // force setting source complete
        InputStream[] tees = in.getTees();

        assertEquals(0, tees[0].read());
        assertEquals(0, tees[1].read());

        readLen = tees[0].read(temp, 0, 5);
        assertEquals(5, readLen);
        assertArrayEquals(Arrays.copyOfRange(data, 1, 6), Arrays.copyOfRange(temp, 0, 5));

        readLen = tees[0].read(temp);
        assertEquals(4, readLen);
        assertArrayEquals(Arrays.copyOfRange(data, 6, 10), Arrays.copyOfRange(temp, 0, 4));

        readLen = tees[1].read(temp);
        assertEquals(9, readLen);
        assertArrayEquals(Arrays.copyOfRange(data, 1, 10), Arrays.copyOfRange(temp, 0, 9));

        in.close();
    }

    @Test
    public void testLoopUntilTeesAreClosed() throws Exception {
        final byte[] data = createMockData(10);
        InputStream source = new ByteArrayInputStream(data);
        TeeInputStream in = new TeeInputStream(source, 2) {
            int loopCount = 0;

            @Override
            protected void loop() throws Exception {
                byte[] temp = new byte[10];
                int read;

                super.loop();
                switch (loopCount) {
                    case 0:
                        read = getTees()[0].read(temp);
                        assertEquals(10, read);
                        assertArrayEquals(data, temp);
                        break;
                    case 1:
                        read = getTees()[1].read(temp);
                        assertEquals(10, read);
                        assertArrayEquals(data, temp);
                        break;
                    case 2:
                        getTees()[0].close();
                        break;
                    case 3:
                        getTees()[1].close();
                        break;
                }
                loopCount++;
            }
        };

        in.loopUntilTeesAreClosed();
    }

    @Test
    public void testReadsWithSmallBufferSize() throws Exception {
        byte[] temp = new byte[100];
        int readLen;
        byte[] data = createMockData(20);
        InputStream source = new ByteArrayInputStream(data);
        TeeInputStream in = new TeeInputStream(source, 2, 10);
        in.loop(); // force a read
        in.loop(); // force setting source complete
        InputStream[] tees = in.getTees();

        assertEquals(10, in.getMaxNonblockingReadLength(0));
        assertEquals(10, in.getMaxNonblockingReadLength(1));
        assertEquals(0, tees[0].read());
        in.loop(); // doesn't move offset since tees[1] hasn't read yet
        assertEquals(9, in.getMaxNonblockingReadLength(0));
        assertEquals(10, in.getMaxNonblockingReadLength(1));

        assertEquals(0, tees[1].read());
        assertEquals(9, in.getMaxNonblockingReadLength(0));
        assertEquals(9, in.getMaxNonblockingReadLength(1));
        in.loop();
        assertEquals(10, in.getMaxNonblockingReadLength(0));
        assertEquals(10, in.getMaxNonblockingReadLength(1));

        readLen = tees[0].read(temp, 0, 10);
        assertEquals(10, readLen);
        assertArrayEquals(Arrays.copyOfRange(data, 1, 11), Arrays.copyOfRange(temp, 0, 10));
        assertEquals(0, in.getMaxNonblockingReadLength(0));
        assertEquals(10, in.getMaxNonblockingReadLength(1));

        readLen = tees[1].read(temp, 0, 10);
        assertEquals(10, readLen);
        assertArrayEquals(Arrays.copyOfRange(data, 1, 11), Arrays.copyOfRange(temp, 0, 10));
        assertEquals(0, in.getMaxNonblockingReadLength(0));
        assertEquals(0, in.getMaxNonblockingReadLength(1));
        in.loop();
        assertEquals(9, in.getMaxNonblockingReadLength(0));
        assertEquals(9, in.getMaxNonblockingReadLength(1));

        in.close();
    }

    @Test
    public void testAsyncLoops() throws Exception {
        byte[] temp = new byte[100];
        int readLen;
        byte[] data = createMockData(10);
        InputStream source = new ByteArrayInputStream(data);
        final TeeInputStream in = new TeeInputStream(source, 2);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    in.loopUntilTeesAreClosed();
                } catch (Exception e) {
                    System.out.println("Fail");
                }
            }
        });
        t.start();
        InputStream[] tees = in.getTees();

        assertEquals(0, tees[0].read());
        Thread.sleep(1);
        assertEquals(0, tees[1].read());
        Thread.sleep(1);

        readLen = tees[0].read(temp, 0, 5);
        Thread.sleep(1);
        assertEquals(5, readLen);
        assertArrayEquals(Arrays.copyOfRange(data, 1, 6), Arrays.copyOfRange(temp, 0, 5));

        readLen = tees[0].read(temp);
        Thread.sleep(1);
        assertEquals(4, readLen);
        assertArrayEquals(Arrays.copyOfRange(data, 6, 10), Arrays.copyOfRange(temp, 0, 4));

        readLen = tees[1].read(temp);
        Thread.sleep(1);
        assertEquals(9, readLen);
        assertArrayEquals(Arrays.copyOfRange(data, 1, 10), Arrays.copyOfRange(temp, 0, 9));

        Thread.sleep(1);
        in.close();
    }

    @Test
    public void testCloseBeforeReadAll() throws Exception {
        byte[] data = createMockData(4);
        InputStream source = new ByteArrayInputStream(data);
        TeeInputStream in = new TeeInputStream(source, 2, 2);
        in.loop();
        InputStream[] tees = in.getTees();

        tees[0].close();
        in.loop();

        assertEquals(0, tees[1].read());
        in.loop();
        assertEquals(1, tees[1].read());
        in.loop();
        assertEquals(2, tees[1].read());
        in.loop();
        assertEquals(3, tees[1].read());
        in.loop();

        in.close();
    }

    private byte[] createMockData(int len) {
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            data[i] = (byte) i;
        }
        return data;
    }
}
