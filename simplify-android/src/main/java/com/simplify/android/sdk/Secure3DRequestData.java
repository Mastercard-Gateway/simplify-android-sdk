package com.simplify.android.sdk;

/**
 *
 */
public class Secure3DRequestData {

    long amount;
    String currency;
    String description;

    /**
     *
     * @return
     */
    public long getAmount() {
        return amount;
    }

    /**
     *
     * @param amount
     * @return
     */
    public Secure3DRequestData setAmount(long amount) {
        this.amount = amount;
        return this;
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
     * @param currency
     * @return
     */
    public Secure3DRequestData setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    /**
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description
     * @return
     */
    public Secure3DRequestData setDescription(String description) {
        this.description = description;
        return this;
    }
}
