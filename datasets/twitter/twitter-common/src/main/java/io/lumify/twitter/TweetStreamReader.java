package io.lumify.twitter;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TweetStreamReader {
    private final BufferedInputStream in;

    public TweetStreamReader(InputStream in) throws IOException {
        this.in = new BufferedInputStream(in);
        skipWhitespaceOrBeginArray();
    }

    public JSONObject read() throws IOException {
        skipToBeginningOfJsonObject();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int bracketCount = 0;
        boolean inString = false;
        while (true) {
            int ch = this.in.read();
            if (ch == -1) {
                return null;
            }

            buffer.write(ch);
            if (ch == '\\') {
                ch = this.in.read();
                buffer.write(ch);
            } else if (ch == '"') {
                inString = !inString;
            } else if (inString) {

            } else if (ch == '{') {
                bracketCount++;
            } else if (ch == '}') {
                bracketCount--;
                if (bracketCount == 0) {
                    return new JSONObject(buffer.toString());
                }
            }
        }
    }

    private void skipToBeginningOfJsonObject() throws IOException {
        while (true) {
            this.in.mark(1);
            int ch = this.in.read();
            if (ch == '{') {
                this.in.reset();
                return;
            }
            if (ch == -1) {
                return;
            }
        }
    }

    private void skipWhitespaceOrBeginArray() throws IOException {
        while (true) {
            this.in.mark(1);
            int ch = this.in.read();
            if (ch == -1) {
                this.in.reset();
                return;
            } else if (ch == '[') {
                return;
            } else if (!Character.isWhitespace(ch)) {
                this.in.reset();
                return;
            }
        }
    }
}
