package com.simplify.android.sdk;


import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.widget.NumberPicker;

import java.lang.reflect.Field;
import java.util.Calendar;

class Utils {

    /**
     * Validates that a card number passes Luhn and matches minimum length
     * requirements for a specific card type
     *
     * @param number The card number
     * @param brand  The card type to validate against
     * @return True or False
     */
    static boolean validateCardNumber(String number, CardBrand brand) {
        if (number == null || brand == null) {
            return false;
        }

        // numbers only, please
        number = number.replaceAll("[^\\d]+", "");

        // match against type prefix
        if (!brand.prefixMatches(number)) {
            return false;
        }

        // ensure minimum length is satisfied
        int length = number.length();
        if (length == 0 || length < brand.getMinLength()) {
            return false;
        }

        int sum = 0;
        for (int i = length - 2; i >= 0; i -= 2) {
            char c = number.charAt(i);
            if (c < '0' || c > '9') return false;

            // Multiply digit by 2.
            int v = (c - '0') << 1;

            // Add each digit independently.
            sum += v > 9 ? 1 + v - 10 : v;
        }

        // Add the rest of the non-doubled digits
        for (int i = length - 1; i >= 0; i -= 2) {
            sum += number.charAt(i) - '0';
        }

        // Double check that the Luhn check-digit at the end brings us to a neat multiple of 10
        return sum % 10 == 0;
    }

    /**
     * Validates that a provided expiration date is in the future
     *
     * @param month The expiration month, format: MM
     * @param year  The expiration year, format: YY
     * @return True or False
     */
    static boolean validateCardExpiration(String month, String year) {
        if (month.trim().length() == 0 || year.trim().length() == 0) {
            return false;
        }

        int intMonth = Integer.parseInt(month);
        int intYear = Integer.parseInt(year);

        if (intYear < 100) {
            intYear += 2000;
        }

        if (intMonth == 0) {
            return false;
        }

        Calendar now = Calendar.getInstance();

        Calendar expire = Calendar.getInstance();
        expire.set(intYear, intMonth - 1, 1, 0, 0, 0);
        expire.add(Calendar.MONTH, 1);

        return now.before(expire);
    }

    /**
     * Validates that a cvc code matches the length required by the card type
     *
     * @param cvc   The cvc code
     * @param brand The card type
     * @return True or False
     */
    static boolean validateCardCvc(String cvc, CardBrand brand) {
        return (cvc != null && cvc.trim().length() == brand.getCvcLength());
    }


    static void setDividerColor(NumberPicker picker, int color) {

        Field[] numberPickerFields = NumberPicker.class.getDeclaredFields();
        for (Field field : numberPickerFields) {
            if (field.getName().equals("mSelectionDivider")) {
                field.setAccessible(true);
                try {
                    field.set(picker, new ColorDrawable(color));
                } catch (IllegalArgumentException | Resources.NotFoundException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
