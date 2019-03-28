package com.simplify.android.sdk;

/**
 * Class representing the 3DS response data after creating a card token
 */
public class Secure3DData {

    String id;
    Boolean isEnrolled;
    String acsUrl;
    String paReq;
    String md;
    String termUrl;

    /**
     * Gets the ID of the 3DS transaction
     *
     * @return The ID
     */
    public String getId() {
        return id;
    }

    /**
     * Indicates whether the card is enrolled in 3DS.
     * <br/>If not enrolled, should not perform 3DS authentication
     *
     * @return True if enrolled, False otherwise
     */
    public Boolean getEnrolled() {
        return isEnrolled;
    }

    /**
     * Gets the ACS URL of the 3DS transaction
     *
     * @return The ACS URL
     */
    public String getAcsUrl() {
        return acsUrl;
    }

    /**
     * Gets the PaReq of the 3DS transaction
     *
     * @return The PaReq
     */
    public String getPaReq() {
        return paReq;
    }

    /**
     * Gets the Merchant Data associated with this 3DS transaction
     *
     * @return The merchant data
     */
    public String getMerchantData() {
        return md;
    }

    /**
     * Gets the Termination URL of the 3DS transaction
     *
     * @return the termination URL
     */
    public String getTermUrl() {
        return termUrl;
    }
}
