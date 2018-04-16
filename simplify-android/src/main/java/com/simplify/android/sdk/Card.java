package com.simplify.android.sdk;

import com.google.gson.JsonObject;

/**
 * Class representing a user-provided card
 */
@SuppressWarnings("unused")
public class Card {

    String id;

    String number;
    String last4;
    String expMonth;
    String expYear;
    String cvc;

    String addressLine1;
    String addressLine2;
    String addressCity;
    String addressState;
    String addressZip;
    String addressCountry;

    CardBrand type;
    Customer customer;

    long dateCreated;

    CardEntryMode cardEntryMode;
    JsonObject androidPayData;


    /**
     * The id of card on file
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * The card number
     * @return The card number
     */
    public String getNumber() {
        return number;
    }

    /**
     * Sets the card number
     * @param number The card number
     * @return The card
     */
    public Card setNumber(String number) {
        this.number = number;
        return this;
    }

    /**
     * The last 4 digits of the card number (is not changed by setting the card number)
     * @return The last 4 digits of the card number
     */
    public String getLast4() {
        return last4;
    }

    /**
     * The expiration month
     * @return The expiration month in format 'MM'
     */
    public String getExpMonth() {
        return expMonth;
    }

    /**
     * Sets the expiration month
     * @param expMonth The expiration month in format 'MM'
     * @return The card
     */
    public Card setExpMonth(String expMonth) {
        this.expMonth = expMonth;
        return this;
    }

    /**
     * The expiration year
     * @return The expiration year in format 'YY'
     */
    public String getExpYear() {
        return expYear;
    }

    /**
     * Sets the expiration year
     * @param expYear The expiration year in format 'YY'
     * @return The card
     */
    public Card setExpYear(String expYear) {
        this.expYear = expYear;
        return this;
    }

    /**
     * The card cvc
     * @return The card cvc
     */
    public String getCvc() {
        return cvc;
    }

    /**
     * Sets the card cvc
     * @param cvc The card cvc
     * @return The card
     */
    public Card setCvc(String cvc) {
        this.cvc = cvc;
        return this;
    }

    /**
     * The address line 1
     * @return The address line 1
     */
    public String getAddressLine1() {
        return addressLine1;
    }

    /**
     * Sets the address line 1
     * @param addressLine1 The address line 1
     * @return The card
     */
    public Card setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
        return this;
    }

    /**
     * The address line 2
     * @return The address line 2
     */
    public String getAddressLine2() {
        return addressLine2;
    }

    /**
     * Sets the address line 2
     * @param addressLine2 The address line 2
     * @return The card
     */
    public Card setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
        return this;
    }

    /**
     * The address city
     * @return The address city
     */
    public String getAddressCity() {
        return addressCity;
    }

    /**
     * Sets the address city
     * @param addressCity The address city
     * @return The card
     */
    public Card setAddressCity(String addressCity) {
        this.addressCity = addressCity;
        return this;
    }

    /**
     * The address state
     * @return The address state
     */
    public String getAddressState() {
        return addressState;
    }

    /**
     * Sets the address state
     * @param addressState The address state
     * @return The card
     */
    public Card setAddressState(String addressState) {
        this.addressState = addressState;
        return this;
    }

    /**
     * The address zip
     * @return The address zip
     */
    public String getAddressZip() {
        return addressZip;
    }

    /**
     * Sets the address zip
     * @param addressZip The address zip
     * @return The card
     */
    public Card setAddressZip(String addressZip) {
        this.addressZip = addressZip;
        return this;
    }

    /**
     * The address country
     * @return The address country
     */
    public String getAddressCountry() {
        return addressCountry;
    }

    /**
     * Sets the address country
     * @param addressCountry The address country
     * @return The card
     */
    public Card setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
        return this;
    }

    /**
     * The card type
     * @return The card type
     */
    public CardBrand getType() {
        return type;
    }

    /**
     * Sets the card type
     * @param type The card type
     * @return The card
     */
    public Card setType(CardBrand type) {
        this.type = type;
        return this;
    }

    /**
     * The customer associated with this card
     * @return The customer
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * Sets the customer associated with this card
     * @param customer The customer
     * @return The card
     */
    public Card setCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    /**
     * The date the card was created
     * @return The date created
     */
    public long getDateCreated() {
        return dateCreated;
    }


    CardEntryMode getCardEntryMode() {
        return cardEntryMode;
    }

    Card setCardEntryMode(CardEntryMode cardEntryMode) {
        this.cardEntryMode = cardEntryMode;
        return this;
    }

    JsonObject getAndroidPayData() {
        return androidPayData;
    }

    Card setAndroidPayData(JsonObject androidPayData) {
        this.androidPayData = androidPayData;
        return this;
    }
}
