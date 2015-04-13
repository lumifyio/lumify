package io.lumify.palantir.model;

import org.apache.hadoop.io.Writable;

import java.io.*;

public abstract class PtModelBase implements Writable {
    public abstract Writable getKey();

    protected String readFieldNullableString(DataInput in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        return in.readUTF();
    }

    protected void writeFieldNullableString(DataOutput out, String str) throws IOException {
        out.writeBoolean(str != null);
        if (str != null) {
            out.writeUTF(str);
        }
    }

    protected Object readFieldNullableObject(DataInput in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        byte[] data = readFieldNullableByteArray(in);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        try {
            return ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not read object", e);
        }
    }

    protected void writeFieldNullableObject(DataOutput out, Object obj) throws IOException {
        out.writeBoolean(obj != null);
        if (obj != null) {
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(data);
            try {
                oos.writeObject(obj);
            } catch (Exception ex) {
                throw new IOException("Could not write object of type: " + obj.getClass().getName(), ex);
            }
            oos.close();
            byte[] dataBytes = data.toByteArray();
            writeFieldNullableByteArray(out, dataBytes);
        }
    }

    protected Long readFieldNullableLong(DataInput in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        return in.readLong();
    }

    protected void writeFieldNullableLong(DataOutput out, Long l) throws IOException {
        out.writeBoolean(l != null);
        if (l != null) {
            out.writeLong(l);
        }
    }

    protected byte[] readFieldNullableByteArray(DataInput in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        int size = in.readInt();
        byte[] data = new byte[size];
        in.readFully(data, 0, size);
        return data;
    }

    protected void writeFieldNullableByteArray(DataOutput out, byte[] data) throws IOException {
        out.writeBoolean(data != null);
        if (data != null) {
            out.writeInt(data.length);
            out.write(data);
        }
    }
}
