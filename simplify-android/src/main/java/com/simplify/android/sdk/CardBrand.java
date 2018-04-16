package com.simplify.android.sdk;

/**
 * Enumerated card brands supported by the Simplify API
 */
public enum CardBrand {

    VISA(13, 19, 3, "^4\\d*"),
    MASTERCARD(16, 16, 3, "^(?:5[1-5]|67)\\d*"),
    AMERICAN_EXPRESS(15, 15, 4, "^3[47]\\d*"),
    DISCOVER(16, 16, 3, "^6(?:011|4[4-9]|5)\\d*"),
    DINERS(14, 16, 3, "^3(?:0(?:[0-5]|9)|[689])\\d*"),
    JCB(16, 16, 3, "^35(?:2[89]|[3-8])\\d*"),
    UNKNOWN(13, 19, 3);


    int minLength;
    int maxLength;
    int cvcLength;
    String pattern;

    CardBrand(int minLength, int maxLength, int cvcLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.cvcLength = cvcLength;
    }

    CardBrand(int minLength, int maxLength, int cvcLength, String prefixPattern) {
        this(minLength, maxLength, cvcLength);
        pattern = prefixPattern;
    }

    int getMinLength() {
        return minLength;
    }

    int getMaxLength() {
        return maxLength;
    }

    int getCvcLength() {
        return cvcLength;
    }

    boolean prefixMatches(String number) {
        return pattern == null || number.matches(pattern);
    }

    String format(String number) {
        if (number == null) {
            throw new IllegalArgumentException("Number cannot be null");
        }

        String formatted = "";
        int length = number.length();

        switch (this) {
            case AMERICAN_EXPRESS:
                for (int i = 0; i < length; i++) {
                    formatted += (i == 4 || i == 10 ? " " : "") + number.charAt(i);
                }
                break;
            default:
                for (int i = 0; i < length; i++) {
                    formatted += (i > 0 && i % 4 == 0 ? " " : "") + number.charAt(i);
                }
                break;
        }

        return formatted;
    }

    static CardBrand detect(String cardNumber) {
        if (cardNumber != null) {
            cardNumber = cardNumber.replaceAll("[^\\d]+", "");
            for (CardBrand brand : values()) {
                if (brand.pattern != null && brand.prefixMatches(cardNumber)) {
                    return brand;
                }
            }
        }

        return UNKNOWN;
    }
}
