package com.simplify.android.sdk.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.BuyButtonAppearance;
import com.google.android.gms.wallet.fragment.BuyButtonText;
import com.google.android.gms.wallet.fragment.Dimension;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import com.simplify.android.sdk.AndroidPayCallback;
import com.simplify.android.sdk.Card;
import com.simplify.android.sdk.CardToken;
import com.simplify.android.sdk.CardEditor;
import com.simplify.android.sdk.Simplify;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    static final String CURRENCY_CODE_USD = "USD";
    static final String WALLET_FRAGMENT_ID = "wallet_fragment";

    CardEditor mCardEditor;

    //callback can be used in onActivityResult to get back MaskedWallet
    AndroidPayCallback mAndroidPayCallback = new AndroidPayCallback(){
        @Override
        public void onReceivedMaskedWallet(MaskedWallet maskedWallet) {
            super.onReceivedMaskedWallet(maskedWallet);
            launchConfirmationActivity(maskedWallet);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        // adds Google Buy button as fragment
        showGoogleWalletButton();
    }

    void initUI() {

        mCardEditor = (CardEditor) findViewById(R.id.card_editor);

        mCardEditor.setOnChargeClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                Card card = mCardEditor.getCard();

                Simplify.createCardToken(card, new CardToken.Callback() {
                    @Override
                    public void onSuccess(CardToken cardToken) {
                        Log.i(TAG, "Created Token: " + cardToken.getId());
                        mCardEditor.showSuccessOverlay("Created card token " + cardToken.getId());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, "Error Creating Token: " + throwable.getMessage());
                        mCardEditor.showErrorOverlay("Unable to retrieve card token. " + throwable.getMessage());
                    }
                });
            }
        });

        // init reset button
        findViewById(R.id.btnReset).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mCardEditor.reset();
            }
        });

        // Redirect onActivityResult() to Simplify to handle MaskedWalletRequest
        // and return masked wallet object.Refer to onActivityResult() method to know how.

        // to subscribe for MaskedWallet using RxJava
        /*Simplify.getMaskedWalletObservable().subscribe(new Action1<MaskedWallet>() {
            @Override
            public void call(MaskedWallet maskedWallet) {
                launchConfirmationActivity(maskedWallet);
            }
        });*/
    }

    void showGoogleWalletButton() {

        // Check if WalletFragment already exists
        SupportWalletFragment walletFragment = (SupportWalletFragment) getSupportFragmentManager()
                .findFragmentByTag(WALLET_FRAGMENT_ID);

        if (walletFragment != null) {
            return;
        }

        //buy button style
        WalletFragmentStyle fragmentStyle = new WalletFragmentStyle()
                .setBuyButtonText(BuyButtonText.BUY_NOW)
                .setBuyButtonAppearance(BuyButtonAppearance.CLASSIC)
                .setBuyButtonWidth(Dimension.MATCH_PARENT);

        //options to set button theme and mode
        WalletFragmentOptions fragmentOptions = WalletFragmentOptions.newBuilder()
                .setEnvironment(WalletConstants.ENVIRONMENT_SANDBOX)
                .setFragmentStyle(fragmentStyle)
                .setTheme(WalletConstants.THEME_HOLO_LIGHT)
                .setMode(WalletFragmentMode.BUY_BUTTON)
                .build();

        // instantiate the WalletFragment
        walletFragment = SupportWalletFragment.newInstance(fragmentOptions);

        //initializing the WalletFragment
        WalletFragmentInitParams.Builder startParamsBuilder = WalletFragmentInitParams.newBuilder()
                .setMaskedWalletRequest(getMaskedWalletRequest())
                //to redirect onActivityResult() to Simplify,
                // using below request code Simplify.REQUEST_CODE_MASKED_WALLET is mandatory
                .setMaskedWalletRequestCode(Simplify.REQUEST_CODE_MASKED_WALLET);

        walletFragment.initialize(startParamsBuilder.build());

        // add Wallet fragment to the UI
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.buy_button_holder, walletFragment, WALLET_FRAGMENT_ID)
                .commit();
    }

    MaskedWalletRequest getMaskedWalletRequest() {

        MaskedWalletRequest maskedWalletRequest =
                MaskedWalletRequest.newBuilder()
                        .setMerchantName("MasterCard labs")
                        .setPhoneNumberRequired(true)
                        .setShippingAddressRequired(true)
                        .setCurrencyCode(CURRENCY_CODE_USD)
                        .setCart(Cart.newBuilder()
                                .setCurrencyCode(CURRENCY_CODE_USD)
                                .setTotalPrice("5.00")
                                .addLineItem(LineItem.newBuilder()
                                        .setCurrencyCode(CURRENCY_CODE_USD)
                                        .setDescription("Mc labs")
                                        .setQuantity("1")
                                        .setUnitPrice("5.00")
                                        .setTotalPrice("5.00")
                                        .build())
                                .build())
                        .setEstimatedTotalPrice("5.00")
                        .build();

        return maskedWalletRequest;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // to get back MaskedWallet using call back method.
        if (Simplify.handleAndroidPayResult(requestCode,resultCode, data, mAndroidPayCallback)) {
            return;
        }

        // to get back MaskedWallet using RxJava, use the code snippet below
        // subscribe to Simplify.getMaskedWalletObservable() to receive maskedwallet object
        // Refer to onCreate() method for code snippet
        /*if (Simplify.handleAndroidPayResult(requestCode,resultCode, data)) {
            return;
        }*/
    }

    void launchConfirmationActivity(MaskedWallet maskedWallet) {
        Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
        intent.putExtra(WalletConstants.EXTRA_MASKED_WALLET, maskedWallet);
        startActivity(intent);
    }

}
