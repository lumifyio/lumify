package io.lumify.core.model.notification;

public class ExpirationAge {
    private int amount;
    private ExpirationAgeUnit expirationAgeUnit;

    public ExpirationAge(int amount, ExpirationAgeUnit expirationAgeUnit) {
        this.amount = amount;
        this.expirationAgeUnit = expirationAgeUnit;
    }

    public int getAmount() {
        return amount;
    }

    public ExpirationAgeUnit getExpirationAgeUnit() {
        return expirationAgeUnit;
    }
}
