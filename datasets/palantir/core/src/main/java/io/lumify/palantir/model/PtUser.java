package io.lumify.palantir.model;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PtUser extends PtModelBase {
    private long id;
    private String login;
    private String password;
    private String mackey;
    private String firstName;
    private String lastName;
    private Long lastLogin;
    private String type;
    private long authSourceId;
    private Long authUid;
    private boolean deleted;
    private long locked;
    private String lockReason;
    private long lockTimestamp;
    private long createdBy;
    private long timeCreated;
    private long lastModified;
    private String email;
    private long emailSource;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMackey() {
        return mackey;
    }

    public void setMackey(String mackey) {
        this.mackey = mackey;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getAuthSourceId() {
        return authSourceId;
    }

    public void setAuthSourceId(long authSourceId) {
        this.authSourceId = authSourceId;
    }

    public Long getAuthUid() {
        return authUid;
    }

    public void setAuthUid(Long authUid) {
        this.authUid = authUid;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public long getLocked() {
        return locked;
    }

    public void setLocked(long locked) {
        this.locked = locked;
    }

    public String getLockReason() {
        return lockReason;
    }

    public void setLockReason(String lockReason) {
        this.lockReason = lockReason;
    }

    public long getLockTimestamp() {
        return lockTimestamp;
    }

    public void setLockTimestamp(long lockTimestamp) {
        this.lockTimestamp = lockTimestamp;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(long createdBy) {
        this.createdBy = createdBy;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getEmailSource() {
        return emailSource;
    }

    public void setEmailSource(long emailSource) {
        this.emailSource = emailSource;
    }

    @Override
    public String toString() {
        return "PtUser{" +
                "id=" + id +
                '}';
    }

    @Override
    public Writable getKey() {
        return new LongWritable(getId());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(getId());
        out.writeUTF(getLogin());
        out.writeUTF(getPassword());
        writeFieldNullableString(out, getMackey());
        out.writeUTF(getFirstName());
        out.writeUTF(getLastName());
        writeFieldNullableLong(out, getLastLogin());
        out.writeUTF(getType());
        out.writeLong(getAuthSourceId());
        writeFieldNullableLong(out, getAuthUid());
        out.writeBoolean(isDeleted());
        out.writeLong(getLocked());
        writeFieldNullableString(out, getLockReason());
        out.writeLong(getLockTimestamp());
        out.writeLong(getCreatedBy());
        out.writeLong(getTimeCreated());
        out.writeLong(getLastModified());
        writeFieldNullableString(out, getEmail());
        out.writeLong(getEmailSource());
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        setId(in.readLong());
        setLogin(in.readUTF());
        setPassword(in.readUTF());
        setMackey(readFieldNullableString(in));
        setFirstName(in.readUTF());
        setLastName(in.readUTF());
        setLastLogin(readFieldNullableLong(in));
        setType(in.readUTF());
        setAuthSourceId(in.readLong());
        setAuthUid(readFieldNullableLong(in));
        setDeleted(in.readBoolean());
        setLocked(in.readLong());
        setLockReason(readFieldNullableString(in));
        setLockTimestamp(in.readLong());
        setCreatedBy(in.readLong());
        setTimeCreated(in.readLong());
        setLastModified(in.readLong());
        setEmail(readFieldNullableString(in));
        setEmailSource(in.readLong());
    }
}
