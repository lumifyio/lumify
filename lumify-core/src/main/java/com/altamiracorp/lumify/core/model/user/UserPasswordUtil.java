package com.altamiracorp.lumify.core.model.user;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public class UserPasswordUtil {
    private static final int SALT_LENGTH = 16;
    private static final int ITERATION_COUNT = 1000;
    private static final int KEY_LENGTH = 64 * 8;
    public static final String SHA_1_PRNG = "SHA1PRNG";

    public static byte[] getSalt() {
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstance(SHA_1_PRNG);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find algorithm: " + SHA_1_PRNG, e);
        }
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    public static byte[] hashPassword(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Invalid key spec", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find algorithm", e);
        }
    }

    public static boolean validatePassword(String password, byte[] salt, byte[] storedPassword) {
        byte[] hashedPassword = hashPassword(password, salt);

        if (hashedPassword.length != storedPassword.length) {
            return false;
        }
        for (int i = 0; i < storedPassword.length; i++) {
            if (hashedPassword[i] != storedPassword[i]) {
                return false;
            }
        }
        return true;
    }
}
