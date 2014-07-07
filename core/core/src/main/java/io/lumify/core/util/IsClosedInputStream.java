package io.lumify.core.util;

import org.apache.commons.io.input.ProxyInputStream;

import java.io.IOException;
import java.io.InputStream;

public class IsClosedInputStream extends ProxyInputStream {
    private boolean closed;

    public IsClosedInputStream(InputStream source) {
        super(source);
        closed = false;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            this.closed = true;
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
