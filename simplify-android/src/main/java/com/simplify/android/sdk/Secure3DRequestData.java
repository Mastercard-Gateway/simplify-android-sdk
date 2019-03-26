package com.simplify.android.sdk;

import android.support.annotation.NonNull;

/**
 *
 */
public class Secure3DRequestData {

    long amount;
    String currency;
    String description;


    public Secure3DRequestData(@NonNull long amount, @NonNull String currency, @NonNull String description) {
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    /**
     *
     * @return
     */
    public long getAmount() {
        return amount;
    }

    /**
     *
     * @return
     */
    public String getCurrency() {
        return currency;
    }

    /**
     *
     * @return
     */
    public String getDescription() {
        return description;
    }
}
