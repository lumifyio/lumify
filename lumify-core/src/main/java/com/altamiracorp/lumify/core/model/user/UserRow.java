package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public class UserRow extends Row<UserRowKey> {
    public static final String TABLE_NAME = "atc_user";

    public UserRow(UserRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public UserRow(RowKey rowKey) {
        super(TABLE_NAME, new UserRowKey(rowKey.toString()));
    }

    public UserRow() {
        super(TABLE_NAME);
    }

    @Override
    public UserRowKey getRowKey() {
        UserRowKey rowKey = super.getRowKey();
        if (rowKey == null) {
            rowKey = new UserRowKey(getMetadata().getUserName());
        }
        return rowKey;
    }

    public UserMetadata getMetadata() {
        UserMetadata userMetadata = get(UserMetadata.NAME);
        if (userMetadata == null) {
            addColumnFamily(new UserMetadata());
        }
        return get(UserMetadata.NAME);
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("rowKey", getRowKey().toString());
            json.put("userName", getMetadata().getUserName());
            json.put("status", getMetadata().getStatus().toString().toLowerCase());
            json.put("userType", getMetadata().getUserType());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPassword(String password) {
        try {
            byte[] salt = getSalt();
            getMetadata().setPasswordSalt(salt);
            getMetadata().setPassword(hashPassword(password, salt));
        } catch (Exception ex) {
            throw new RuntimeException("error setting password", ex);
        }
    }

    public boolean isPasswordValid(String password) {
        try {
            return validatePassword(password, getMetadata().getPasswordSalt(), getMetadata().getPassword());
        } catch (Exception ex) {
            throw new RuntimeException("error validating password", ex);
        }
    }

    private static final int SALT_LENGTH = 16;
    private static final int ITERATION_COUNT = 1000;
    private static final int KEY_LENGTH = 64 * 8;

    private byte[] getSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    private byte[] hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return skf.generateSecret(spec).getEncoded();
    }

    private boolean validatePassword(String password, byte[] salt, byte[] storedPassword) throws NoSuchAlgorithmException, InvalidKeySpecException {
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
