package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class UserRow extends Row<UserRowKey> {
    public static final String TABLE_NAME = "atc_user";
    private static Random random = new Random();

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
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] salt = createSalt();
            md.update(salt);
            md.update(password.getBytes());
            getMetadata().setPasswordSalt(salt);
            getMetadata().setPassword(md.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Could not find md5", ex);
        }
    }

    private byte[] createSalt() {
        byte[] salt = new byte[4];
        random.nextBytes(salt);
        return salt;
    }

    public boolean isPasswordValid(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(getMetadata().getPasswordSalt());
            md.update(password.getBytes());
            byte[] existingPassword = getMetadata().getPassword();
            byte[] checkPassword = md.digest();
            if (existingPassword.length != checkPassword.length) {
                return false;
            }
            for (int i = 0; i < existingPassword.length; i++) {
                if (existingPassword[i] != checkPassword[i]) {
                    return false;
                }
            }
            return true;
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Could not find md5", ex);
        }
    }
}
