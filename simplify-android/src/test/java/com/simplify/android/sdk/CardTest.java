package com.simplify.android.sdk;


import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class CardTest {

    private Card mCard;

    @Before
    public void setUp() throws Exception {
        mCard = new Card();
    }

    @Test
    public void testCardNumbersAreFormattedCorrectly() {
        // test set number/unknown type
        mCard.setNumber("1234567890");
        assertEquals(mCard.getNumber(), "1234567890");

        // map out test formats
        HashMap<CardBrand, String[]> numbers = new HashMap<>();
        numbers.put(CardBrand.AMERICAN_EXPRESS, new String[]{
                "371449635398431",
                "3714 496353 98431"
        });
        numbers.put(CardBrand.DINERS, new String[]{
                "30569309025904",
                "3056 9309 0259 04"
        });
        numbers.put(CardBrand.DISCOVER, new String[]{
                "6011000990139424",
                "6011 0009 9013 9424"
        });
        numbers.put(CardBrand.JCB, new String[]{
                "3530111333300000",
                "3530 1113 3330 0000"
        });
        numbers.put(CardBrand.MASTERCARD, new String[]{
                "5105105105105100",
                "5105 1051 0510 5100"
        });
        numbers.put(CardBrand.VISA, new String[]{
                "4111111111111111",
                "4111 1111 1111 1111"
        });

        // test each formatting
        for (CardBrand brand : CardBrand.values()) {
            if (brand != CardBrand.UNKNOWN) {
                mCard.setNumber(numbers.get(brand)[0]);

                assertEquals(brand.format(mCard.getNumber()), numbers.get(brand)[1]);
            }
        }
    }
}
