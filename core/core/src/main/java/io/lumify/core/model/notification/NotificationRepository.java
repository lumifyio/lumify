package io.lumify.core.model.notification;

import io.lumify.core.exception.LumifyException;
import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class NotificationRepository {

    protected static String hash(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(s.getBytes());
            return Hex.encodeHexString(md5);
        } catch (NoSuchAlgorithmException e) {
            throw new LumifyException("Could not find MD5", e);
        }
    }

}
