package io.lumify.util;

import java.io.IOException;
import java.io.Reader;

public class LineReader {
    private int offset;
    private final Reader reader;

    public LineReader(Reader reader) {
        this.reader = reader;
        this.offset = 0;
    }

    public String readLine() throws IOException {
        StringBuilder line = new StringBuilder();
        while (true) {
            int ch = this.reader.read();
            if (ch == -1) {
                if (line.length() == 0) {
                    return null;
                }
                break;
            }
            this.offset++;
            line.append((char) ch);
            if (ch == '\n') {
                break;
            }
        }
        return line.toString();
    }

    /**
     * Skips over the next <code>skipCount</code> lines in the
     * input.
     * @param skipCount the number of lines to skip
     * @throws IOException
     */
    public void skipLines(final int skipCount) throws IOException {
        for (int ct=0; ct < skipCount; ct++) {
            if (readLine() == null) {
                // we've reached the end of the stream, short-circuit
                // the loop
                break;
            }
        }
    }

    public int getOffset() {
        return offset;
    }
}
