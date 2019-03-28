package com.simplify.android.sdk;

import androidx.annotation.NonNull;

/**
 * Class representing 3DS request data when creating a card token
 */
public class Secure3DRequestData {

    long amount;
    String currency;
    String description;


    /**
     * Cunstructs a new 3DS request data object
     *
     * @param amount      The amount of the transaction
     * @param currency    The currency of the transaction
     * @param description A description of the transaction
     */
    public Secure3DRequestData(@NonNull long amount, @NonNull String currency, @NonNull String description) {
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    /**
     * Gets the amount of the transaction
     *
     * @return The amount
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Gets the currency of the transaction
     *
     * @return The currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Gets the description of the transaction
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }
}
