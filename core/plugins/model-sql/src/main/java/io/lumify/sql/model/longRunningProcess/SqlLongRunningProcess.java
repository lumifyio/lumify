package io.lumify.sql.model.longRunningProcess;

import io.lumify.sql.model.user.SqlUser;

import javax.persistence.*;

@Entity
@Table(name = "long_running_process")
public class SqlLongRunningProcess {
    private String longRunningProcessId;
    private SqlUser user;
    private Long startTime;
    private Long endTime;
    private boolean erred;
    private boolean canceled;
    private String json;

    @Id
    @Column(name = "long_running_process_id", unique = true)
    public String getLongRunningProcessId() {
        return longRunningProcessId;
    }

    public void setLongRunningProcessId(String longRunningProcessId) {
        this.longRunningProcessId = longRunningProcessId;
    }

    @OneToOne
    @JoinColumn(referencedColumnName = "user_id", name = "user_id")
    public SqlUser getUser() {
        return user;
    }

    public void setUser(SqlUser user) {
        this.user = user;
    }

    @Column(name = "json", length = 4000)
    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    @Column(name = "start_time")
    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    @Column(name = "end_time")
    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    @Column(name = "canceled")
    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Column(name = "erred")
    public boolean isErred() {
        return erred;
    }

    public void setErred(boolean erred) {
        this.erred = erred;
    }
}
