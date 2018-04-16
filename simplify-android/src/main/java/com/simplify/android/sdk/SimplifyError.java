package com.simplify.android.sdk;

import java.util.List;

/**
 * Class representing a Simplify API error
 */
@SuppressWarnings("unused")
public class SimplifyError extends RuntimeException {

    int statusCode;
    String code;
    String message;
    String reference;
    List<FieldError> fieldErrors;


    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getReference() {
        return reference;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
