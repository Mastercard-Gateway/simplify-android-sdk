package com.simplify.android.sdk;


import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

    @Test
    public void testValidateCardNumber() {
        // test good number
        String number = "5555 5555 5555 4444";
        assertTrue(Utils.validateCardNumber(number, CardBrand.MASTERCARD));

        // test wrong type
        assertFalse(Utils.validateCardNumber(number, CardBrand.VISA));

        // test number too short
        assertFalse(Utils.validateCardNumber(number.substring(0, number.length() - 3), CardBrand.MASTERCARD));

        // test invalid luhn check
        number = "5555 2894 2838 8302";
        assertFalse(Utils.validateCardNumber(number, CardBrand.MASTERCARD));
    }

    @Test
    public void testValidateCardExpiration() {
        // test missing data
        assertFalse(Utils.validateCardExpiration("", ""));

        // test good strings
        assertTrue(Utils.validateCardExpiration("4", "50"));
    }

    @Test
    public void testValidateCardCvc() {
        // test good cvc
        assertTrue(Utils.validateCardCvc("123", CardBrand.MASTERCARD));

        // test bad cvc
        assertFalse(Utils.validateCardCvc("123", CardBrand.AMERICAN_EXPRESS));
    }

    // TODO commented until we mock Base64 static methods
//    @Test
//    public void testReadPemCertificate() throws Exception {
//        Utils.readCertificate(Constants.INTERMEDIATE_CA);
//    }

    // TODO commented until we mock Base64 static methods
//    @Test
//    public void testCreateSSLKeyStore() throws Exception {
//        KeyStore ks = Utils.createSSLKeyStore();
//
//        assertTrue(ks.containsAlias(Constants.KEYSTORE_CA_ALIAS));
//    }
}
