package io.lumify.core.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RowKeyHelper {
    public static final char MINOR_FIELD_SEPARATOR = (char) 0x1f;
    public static final char MAJOR_FIELD_SEPARATOR = (char) 0x1e;
    public static final int OFFSET_WIDTH = 16;

    public static String buildMinor(String... parts) {
        return StringUtils.join(parts, MINOR_FIELD_SEPARATOR);
    }

    public static String buildMajor(String... parts) {
        return StringUtils.join(parts, MAJOR_FIELD_SEPARATOR);
    }

    public static String buildSHA256KeyString(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] sha = digest.digest(bytes);
            return "urn" + MINOR_FIELD_SEPARATOR + "sha256" + MINOR_FIELD_SEPARATOR + Hex.encodeHexString(sha);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String buildSHA256KeyString(InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int bufferSize = 4096;
            byte[] buffer = new byte[bufferSize];
            int read;
            while ((read = in.read(buffer, 0, buffer.length)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] sha = digest.digest();
            return "urn" + MINOR_FIELD_SEPARATOR + "sha256" + MINOR_FIELD_SEPARATOR + Hex.encodeHexString(sha);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String buildSHA256KeyString(InputStream in, OutputStream out) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int bufferSize = 4096;
            byte[] buffer = new byte[bufferSize];
            int read;
            while ((read = in.read(buffer, 0, buffer.length)) > 0) {
                digest.update(buffer, 0, read);
                out.write(buffer, 0, read);
            }
            byte[] sha = digest.digest();
            return "urn" + MINOR_FIELD_SEPARATOR + "sha256" + MINOR_FIELD_SEPARATOR + Hex.encodeHexString(sha);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] splitOnMinorFieldSeperator(String rowKey) {
        return rowKey.split("" + MINOR_FIELD_SEPARATOR);
    }

    public static String[] splitOnMajorFieldSeperator(String rowKey) {
        return rowKey.split("" + MAJOR_FIELD_SEPARATOR);
    }

    public static String jsonEncode(String rowKey) {
        return rowKey.replaceAll("\\x1f", "\\\\x1F");
    }

    public static String jsonDecode(String rowKey) {
        return rowKey.replaceAll("\\\\x1F", "" + MINOR_FIELD_SEPARATOR);
    }
}
