package com.simplify.android.sdk;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

@SuppressWarnings("unused")
class Response {

    HttpURLConnection connection;
    boolean socketTimeout = false;
    Exception exception;
    int statusCode = 0;
    String payload = "";


    Response() {
    }

    Response(HttpURLConnection conn) {
        if (conn == null) {
            throw new IllegalArgumentException();
        }

        connection = conn;

        try {
            statusCode = connection.getResponseCode();

            // if connection has output stream, get the data
            if (connection.getDoInput()) {
                InputStream is = isOk() ? connection.getInputStream() : connection.getErrorStream();
                payload = inputStreamToString(is);
                is.close();
            }
        }
        // flag for socket timeout happens when you try to read the input stream
        // so you have to detect that here
        catch (IOException e) {
            e.printStackTrace();
            socketTimeout = (e instanceof SocketTimeoutException);
            exception = e;
        }
    }

    public Response(Exception e) {
        exception = e;
    }

    public HttpURLConnection getConnection() {
        return connection;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isSocketTimeout() {
        return socketTimeout;
    }

    public boolean hasException() {
        return exception != null;
    }

    public Exception getException() {
        return exception;
    }

    public boolean isOk() {
        return (statusCode >= 200 && statusCode < 300);
    }


    String inputStreamToString(InputStream is) throws IOException {
        // get buffered reader from stream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        // read stream into string builder
        StringBuilder total = new StringBuilder();

        String line;
        while ((line = rd.readLine()) != null) {
            total.append(line);
        }

        return total.toString();
    }
}
