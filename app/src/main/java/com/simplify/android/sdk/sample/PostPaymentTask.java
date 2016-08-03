package com.simplify.android.sdk.sample;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.simplify.android.sdk.CardToken;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PostPaymentTask extends AsyncTask<CardToken, Void, Boolean> {

    static final String TAG = PostPaymentTask.class.getSimpleName();
    static final String HEROKU_URL = "https://android-pay-test.herokuapp.com/charge.php";

    Context context;
    String amount;

    public PostPaymentTask(Context context, String amount) {
        this.context = context;
        this.amount = amount;
    }

    @Override
    protected Boolean doInBackground(CardToken... params) {


        URL url = null;
        HttpURLConnection con = null;
        try {
            url = new URL(HEROKU_URL);

            // build connection
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Android");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            String postData = "simplifyToken="+params[0].getId()+"&amount=" + amount;

            // send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(postData);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            Log.i(TAG, "Sending 'POST' request to URL: " + url);
            Log.i(TAG, "Data: " + postData);
            Log.i(TAG, "Response code: " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            Log.i(TAG, response.toString());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);

        Intent i = new Intent(context, ThankYouActivity.class);
        i.putExtra(ThankYouActivity.EXTRA_PAGE, aBoolean ? ThankYouActivity.PAGE_SUCCESS : ThankYouActivity.PAGE_FAIL);
        context.startActivity(i);
    }
}
