package com.simplify.android.sdk.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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

public class ConfirmationActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Simplify.AndroidPayCallback {

    static final String TAG = ConfirmationActivity.class.getSimpleName();
    static final String CURRENCY_CODE_USD = "USD";

    GoogleApiClient mGoogleApiClient;
    MaskedWallet mMaskedWallet;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        //google api client required to request full wallet
        mGoogleApiClient = getGoogleApiClient();

        mMaskedWallet = getIntent().getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);

        showConfirmationScreen();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Simplify.addAndroidPayCallback(this);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        Simplify.removeAndroidPayCallback(this);

        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Simplify.handleAndroidPayResult(requestCode,resultCode, data)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    void showConfirmationScreen() {

        //fragment style for confirmation screen
        WalletFragmentStyle walletFragmentStyle = new WalletFragmentStyle()
                .setMaskedWalletDetailsBackgroundColor(
                        ContextCompat.getColor(this, android.R.color.white))
                .setMaskedWalletDetailsButtonBackgroundResource(
                        android.R.color.holo_orange_dark)
                .setMaskedWalletDetailsLogoTextColor(
                        ContextCompat.getColor(this, android.R.color.black));

        WalletFragmentOptions walletFragmentOptions = WalletFragmentOptions.newBuilder()
                .setEnvironment(WalletConstants.ENVIRONMENT_SANDBOX)
                .setFragmentStyle(walletFragmentStyle)
                .setTheme(WalletConstants.THEME_HOLO_LIGHT)
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

    GoogleApiClient getGoogleApiClient() {

        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_SANDBOX)
                        .setTheme(WalletConstants.THEME_HOLO_LIGHT)
                        .build())
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}


    @Override
    public void onReceivedMaskedWallet(MaskedWallet maskedWallet) {
        mMaskedWallet = maskedWallet;
    }

    @Override
    public void onReceivedFullWallet(FullWallet fullWallet) {
        // Use fullwallet object to create token
        if(fullWallet != null) {

            Simplify.createAndroidPayCardToken(fullWallet, new CardToken.Callback() {
                @Override
                public void onSuccess(CardToken cardToken) {
                    Log.i(TAG, "Card token created");
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e(TAG, "Error Creating Token: " + throwable.getMessage());
                }
            });
        }
    }

    @Override
    public void onAndroidPayCancelled() {

    }

    @Override
    public void onAndroidPayError(int errorCode) {

    }

    public void onPurchaseConfirm(View view) {

        if (mMaskedWallet == null) {
            Toast.makeText(this, "No masked wallet, can't confirm", Toast.LENGTH_SHORT).show();
            return;
        }

        Wallet.Payments.loadFullWallet(mGoogleApiClient, getFullWalletRequest(), Simplify.REQUEST_CODE_FULL_WALLET);
    }

    private FullWalletRequest getFullWalletRequest() {

        return FullWalletRequest.newBuilder()
                .setGoogleTransactionId(mMaskedWallet.getGoogleTransactionId())
                .setCart(Cart.newBuilder()
                        .setCurrencyCode(CURRENCY_CODE_USD)
                        .setTotalPrice("10.10")
                        .addLineItem(LineItem.newBuilder()
                                .setCurrencyCode(CURRENCY_CODE_USD)
                                .setDescription("Google I/O Sticker")
                                .setQuantity("1")
                                .setUnitPrice("10.00")
                                .setTotalPrice("10.00")
                                .build())
                        .addLineItem(LineItem.newBuilder()
                                .setCurrencyCode(CURRENCY_CODE_USD)
                                .setDescription("Tax")
                                .setRole(LineItem.Role.TAX)
                                .setTotalPrice(".10")
                                .build())
                        .build())
                .build();
    }
}
