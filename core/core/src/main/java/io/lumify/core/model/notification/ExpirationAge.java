package io.lumify.core.model.notification;

public class ExpirationAge {
    private int amount;
    private int calendarUnit;

    public ExpirationAge(int amount, int calendarUnit) {
        this.amount = amount;
        this.calendarUnit = calendarUnit;
    }

    public int getAmount() {
        return amount;
    }

    public int getCalendarUnit() {
        return calendarUnit;
    }
}
