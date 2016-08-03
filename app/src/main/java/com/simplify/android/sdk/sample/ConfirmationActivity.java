package com.simplify.android.sdk.sample;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import com.simplify.android.sdk.CardToken;
import com.simplify.android.sdk.Simplify;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConfirmationActivity extends AppCompatActivity implements Simplify.AndroidPayCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static final String TAG = ConfirmationActivity.class.getSimpleName();

    GoogleApiClient mGoogleApiClient;
    MaskedWallet mMaskedWallet;
    Button mPayButton;
    Simplify simplify;


    //---------------------------------------------
    // Life-Cycle
    //---------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        mMaskedWallet = getIntent().getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();

        simplify.addAndroidPayCallback(this);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        simplify.removeAndroidPayCallback(this);

        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // let the Simplify SDK marshall out the android pay activity results
        if (simplify.handleAndroidPayResult(requestCode, resultCode, data)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    //---------------------------------------------
    // Android Pay callback methods
    //---------------------------------------------

    @Override
    public void onReceivedMaskedWallet(MaskedWallet maskedWallet) {

    }

    @Override
    public void onReceivedFullWallet(FullWallet fullWallet) {

        // create simplify token with wallet
        simplify.createAndroidPayCardToken(fullWallet, new CardToken.Callback() {
            @Override
            public void onSuccess(CardToken cardToken) {
                mPayButton.setEnabled(true);
                new PostPaymentTask().execute(cardToken);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();

                mPayButton.setEnabled(true);

                Intent i = new Intent(ConfirmationActivity.this, ThankYouActivity.class);
                i.putExtra(ThankYouActivity.EXTRA_PAGE, ThankYouActivity.PAGE_FAIL);
                startActivity(i);
            }
        });
    }

    @Override
    public void onAndroidPayCancelled() {

    }

    @Override
    public void onAndroidPayError(int errorCode) {
        Log.e(TAG, "Android Pay error code: " + errorCode);
    }


    //---------------------------------------------
    // Google API Client callback methods
    //---------------------------------------------

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    //---------------------------------------------
    // Util
    //---------------------------------------------

    void init() {

        simplify = ((SimplifyApplication) getApplication()).getSimplify();

        //google api client required to request full wallet
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(Constants.WALLET_ENVIRONMENT)
                        .setTheme(WalletConstants.THEME_LIGHT)
                        .build())
                .build();

        // init pay button
        mPayButton = (Button) findViewById(R.id.btn_pay);
        mPayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmPurchase();
            }
        });

        //fragment style for confirmation screen
        WalletFragmentStyle walletFragmentStyle = new WalletFragmentStyle()
                .setMaskedWalletDetailsBackgroundColor(
                        ContextCompat.getColor(this, android.R.color.white))
                .setMaskedWalletDetailsButtonBackgroundResource(
                        android.R.color.holo_orange_dark);

        WalletFragmentOptions walletFragmentOptions = WalletFragmentOptions.newBuilder()
                .setEnvironment(Constants.WALLET_ENVIRONMENT)
                .setFragmentStyle(walletFragmentStyle)
                .setTheme(WalletConstants.THEME_LIGHT)
                .setMode(WalletFragmentMode.SELECTION_DETAILS)
                .build();

        SupportWalletFragment walletFragment = SupportWalletFragment.newInstance(walletFragmentOptions);

        WalletFragmentInitParams startParams = WalletFragmentInitParams.newBuilder()
                .setMaskedWallet(mMaskedWallet)
                .setMaskedWalletRequestCode(Simplify.REQUEST_CODE_MASKED_WALLET)
                .build();

        walletFragment.initialize(startParams);

        // add Wallet fragment to the UI
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.confirm_wallet_holder, walletFragment)
                .commit();
    }

    void confirmPurchase() {

        mPayButton.setEnabled(false);

        if (mMaskedWallet == null) {
            Toast.makeText(this, "No masked wallet, can't confirm", Toast.LENGTH_SHORT).show();
            return;
        }

        Wallet.Payments.loadFullWallet(mGoogleApiClient, getFullWalletRequest(), Simplify.REQUEST_CODE_FULL_WALLET);
    }

    FullWalletRequest getFullWalletRequest() {

        return FullWalletRequest.newBuilder()
                .setGoogleTransactionId(mMaskedWallet.getGoogleTransactionId())
                .setCart(Cart.newBuilder()
                        .setCurrencyCode(Constants.CURRENCY_CODE_USD)
                        .setTotalPrice("15.00")
                        .addLineItem(LineItem.newBuilder()
                                .setCurrencyCode(Constants.CURRENCY_CODE_USD)
                                .setDescription("Iced Coffee")
                                .setQuantity("1")
                                .setUnitPrice("15.00")
                                .setTotalPrice("15.00")
                                .build())
                        .build())
                .build();
    }


    class PostPaymentTask extends AsyncTask<CardToken, Void, Boolean> {

        @Override
        protected Boolean doInBackground(CardToken... params) {


            URL url = null;
            HttpURLConnection con = null;
            try {
                url = new URL("https://android-pay-test.herokuapp.com/charge.php");

                // build connection
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", "Android");
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                String postData = "simplifyToken="+params[0].getId()+"&amount=1500";

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

            Intent i = new Intent(ConfirmationActivity.this, ThankYouActivity.class);
            i.putExtra(ThankYouActivity.EXTRA_PAGE, aBoolean ? ThankYouActivity.PAGE_SUCCESS : ThankYouActivity.PAGE_FAIL);
            startActivity(i);
        }
    }
}
