package io.lumify.core.util;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HdfsLimitOutputStreamTest {
    @Mock
    private FileSystem mockFileSystem;

    @Mock
    private FSDataOutputStream mockFSDataOutputStream;

    @Test
    public void testSmall() throws NoSuchAlgorithmException, IOException {
        HdfsLimitOutputStream out = new HdfsLimitOutputStream(mockFileSystem, 10);
        byte[] data = createMockData(10);
        out.write(data[0]);
        out.write(data, 1, 2);
        out.write(Arrays.copyOfRange(data, 3, 10));
        out.flush();
        out.close();
        assertEquals(false, out.hasExceededSizeLimit());
        assertArrayEquals(data, out.getSmall());
        assertEquals("urn\u001Fsha256\u001F1f825aa2f0020ef7cf91dfa30da4668d791c5d4824fc8e41354b89ec05795ab3", out.getRowKey());
    }

    @Test
    public void testLarge() throws NoSuchAlgorithmException, IOException {
        ArgumentCaptor<Path> path = ArgumentCaptor.forClass(Path.class);
        when(mockFileSystem.create(path.capture())).thenReturn(mockFSDataOutputStream);
        HdfsLimitOutputStream out = new HdfsLimitOutputStream(mockFileSystem, 2);
        byte[] data = createMockData(11);
        out.write(data[0]);
        out.write(data, 1, 2);
        out.write(Arrays.copyOfRange(data, 3, 10));
        out.write(data[10]);
        out.flush();
        out.close();
        assertEquals(true, out.hasExceededSizeLimit());
        assertEquals(null, out.getSmall());
        assertEquals("urn\u001Fsha256\u001F78a6273103d17c39a0b6126e226cec70e33337f4bc6a38067401b54a33e78ead", out.getRowKey());
        assertEquals(path.getValue(), out.getHdfsPath());
    }

    private byte[] createMockData(int len) {
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            data[i] = (byte) i;
        }
        return data;
    }
}