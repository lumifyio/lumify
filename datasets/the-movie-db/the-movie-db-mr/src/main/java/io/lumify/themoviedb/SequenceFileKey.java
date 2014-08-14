package io.lumify.themoviedb;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SequenceFileKey implements Writable {
    private RecordType recordType;
    private int id;
    private String imagePath;
    private String title;

    public SequenceFileKey() {

    }

    public SequenceFileKey(RecordType type, int id, String imagePath, String title) {
        this.recordType = type;
        this.id = id;
        this.imagePath = imagePath;
        this.title = title;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public int getId() {
        return id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(getRecordType().ordinal());
        out.writeInt(getId());
        String s = getImagePath();
        out.writeUTF(s == null ? "" : s);
        s = getTitle();
        out.writeUTF(s == null ? "" : s);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.recordType = RecordType.values()[in.readInt()];
        this.id = in.readInt();
        this.imagePath = in.readUTF();
        this.title = in.readUTF();
    }

    @Override
    public String toString() {
        return "SequenceFileKey{" +
                "recordType=" + recordType +
                ", id=" + id +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }
}
