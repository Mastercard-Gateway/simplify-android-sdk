package com.simplify.android.sdk;

/**
 *
 */
public class Secure3DData {

    String id;
    Boolean isEnrolled;
    String acsUrl;
    String paReq;
    String md;
    String termUrl;

    /**
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @return
     */
    public Boolean getEnrolled() {
        return isEnrolled;
    }

    /**
     *
     * @return
     */
    public String getAcsUrl() {
        return acsUrl;
    }

    /**
     *
     * @return
     */
    public String getPaReq() {
        return paReq;
    }

    /**
     *
     * @return
     */
    public String getMerchantData() {
        return md;
    }

    /**
     *
     * @return
     */
    public String getTermUrl() {
        return termUrl;
    }
}
