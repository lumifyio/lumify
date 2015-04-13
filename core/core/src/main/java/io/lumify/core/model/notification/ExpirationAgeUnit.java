package io.lumify.core.model.notification;

import java.util.Calendar;

public enum ExpirationAgeUnit {
    SECOND(Calendar.SECOND, "SECOND", "SECOND"),
    MINUTE(Calendar.MINUTE, "MINUTE", "MINUTE"),
    HOUR(Calendar.HOUR, "HOUR", "HOUR"),
    DAY(Calendar.DAY_OF_WEEK, "DAY", "DAY");

    private int calendarUnit;
    private String mysqlInterval;
    private String h2unit;

    private ExpirationAgeUnit(int calendarUnit, String mysqlInterval, String h2unit) {
        this.calendarUnit = calendarUnit;
        this.mysqlInterval = mysqlInterval;
        this.h2unit = h2unit;
    }

    public int getCalendarUnit() {
        return calendarUnit;
    }

    public String getMysqlInterval() {
        return mysqlInterval;
    }

    public String getH2unit() {
        return h2unit;
    }
}
