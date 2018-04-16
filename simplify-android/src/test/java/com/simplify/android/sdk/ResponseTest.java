package com.simplify.android.sdk;


import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ResponseTest {

    static final String testString = "here is a test string";

    Response response;

    @Before
    public void setup() {
        response = new Response();
    }

    @Test
    public void testInputStreamToString() {
        // init a fake stream
        InputStream is = new ByteArrayInputStream(testString.getBytes());

        // test parsing back into string
        try {
            assertEquals(response.inputStreamToString(is), testString);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
