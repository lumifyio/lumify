package io.lumify.palantir.model;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class LongLongWritable implements WritableComparable<LongLongWritable> {
    private long value1;
    private long value2;

    public LongLongWritable() {
    }

    public LongLongWritable(long value1, long value2) {
        set(value1, value2);
    }

    public void set(long value1, long value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    public long get1() {
        return value1;
    }

    public long get2() {
        return value2;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        value1 = in.readLong();
        value2 = in.readLong();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(value1);
        out.writeLong(value2);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LongLongWritable)) {
            return false;
        }
        LongLongWritable other = (LongLongWritable) o;
        return this.value1 == other.value1 && this.value2 == other.value2;
    }

    @Override
    public int hashCode() {
        return (int) (value1 ^ value2);
    }

    @Override
    public int compareTo(LongLongWritable o) {
        int r = (this.value1 < o.value1 ? -1 : (this.value1 == o.value1 ? 0 : 1));
        if (r != 0) {
            return r;
        }
        return (this.value2 < o.value2 ? -1 : (this.value2 == o.value2 ? 0 : 1));
    }

    @Override
    public String toString() {
        return Long.toString(value1) + ":" + Long.toString(value2);
    }

    public static class Comparator extends WritableComparator {
        public Comparator() {
            super(LongLongWritable.class);
        }

        @Override
        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            long thisValue = readLong(b1, s1);
            long thatValue = readLong(b2, s2);
            int r = (thisValue < thatValue ? -1 : (thisValue == thatValue ? 0 : 1));
            if (r != 0) {
                return r;
            }
            thisValue = readLong(b1, s1 + 8);
            thatValue = readLong(b2, s2 + 8);
            return (thisValue < thatValue ? -1 : (thisValue == thatValue ? 0 : 1));
        }
    }

    public static class DecreasingComparator extends Comparator {
        @Override
        public int compare(WritableComparable a, WritableComparable b) {
            return -super.compare(a, b);
        }

        @Override
        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            return -super.compare(b1, s1, l1, b2, s2, l2);
        }
    }

    static {
        WritableComparator.define(LongLongWritable.class, new Comparator());
    }

}
