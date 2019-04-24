package com.simplify.android.sdk.sample;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class GooglePay {

    static String merchantName;
    static String publicKey;
    static String currency;

    static JSONObject getBaseRequest() throws JSONException {
        return new JSONObject()
                .put("apiVersion", 2)
                .put("apiVersionMinor", 0);
    }

    static JSONObject getTokenizationSpecification() throws JSONException {
        return new JSONObject()
                .put("type", "DIRECT")
                .put("parameters", new JSONObject()
                        .put("protocolVersion", "ECv2")
                        .put("publicKey", publicKey));
    }

    static JSONArray getAllowedCardNetworks() {
        return new JSONArray()
                .put("AMEX")
                .put("DISCOVER")
                .put("MASTERCARD")
                .put("VISA");
    }

    static JSONArray getAllowedCardAuthMethods() {
        return new JSONArray()
                .put("PAN_ONLY")
                .put("CRYPTOGRAM_3DS");
    }

    static JSONObject getBaseCardPaymentMethod() throws JSONException {
        return new JSONObject()
                .put("type", "CARD")
                .put("parameters", new JSONObject()
                        .put("allowedAuthMethods", getAllowedCardAuthMethods())
                        .put("allowedCardNetworks", getAllowedCardNetworks()));
    }

    static JSONObject getCardPaymentMethod() throws JSONException {
        return getBaseCardPaymentMethod()
                .put("tokenizationSpecification", getTokenizationSpecification());
    }

    static JSONObject getIsReadyToPayRequest() throws JSONException {
        return getBaseRequest()
                .put("allowedPaymentMethods", new JSONArray()
                        .put(getBaseCardPaymentMethod()));
    }

    static JSONObject getTransactionInfo(String price) throws JSONException {
        return new JSONObject()
                .put("totalPrice", price)
                .put("totalPriceStatus", "FINAL")
                .put("currencyCode", currency);
    }

    static JSONObject getMerchantInfo() throws JSONException {
        return new JSONObject()
                .put("merchantName", merchantName);
    }

    static JSONObject getPaymentDataRequest(String price) throws JSONException {
        return getBaseRequest()
                .put("allowedPaymentMethods", new JSONArray()
                        .put(getCardPaymentMethod()))
                .put("transactionInfo", getTransactionInfo(price))
                .put("merchantInfo", getMerchantInfo());
    }
}
