package com.simplify.android.sdk.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.WalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import com.simplify.android.sdk.Card;
import com.simplify.android.sdk.CardEditor;
import com.simplify.android.sdk.CardToken;
import com.simplify.android.sdk.Simplify;

public class MainActivity extends AppCompatActivity implements Simplify.AndroidPayCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CURRENCY_CODE_USD = "USD";
    private static final String WALLET_FRAGMENT_ID = "wallet_fragment";

    private CardEditor mCardEditor;
    private Button mPayButton;
    private GoogleApiClient mGoogleApiClient;

    //---------------------------------------------
    // Life-Cycle
    //---------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // register Android Pay callback
        Simplify.addAndroidPayCallback(this);

        // connect to google api client
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        // disconnect from google api client
        mGoogleApiClient.disconnect();

        // remove Android Pay callback
        Simplify.removeAndroidPayCallback(this);

        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // let the Simplify SDK marshall out the android pay activity results
        if (Simplify.handleAndroidPayResult(requestCode, resultCode, data)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    //---------------------------------------------
    // Android Pay callback methods
    //---------------------------------------------

    @Override
    public void onReceivedMaskedWallet(MaskedWallet maskedWallet) {
        // launch confirmation activity
        Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
        intent.putExtra(WalletConstants.EXTRA_MASKED_WALLET, maskedWallet);
        startActivity(intent);
    }

    @Override
    public void onReceivedFullWallet(FullWallet fullWallet) {

    }

    @Override
    public void onAndroidPayCancelled() {

    }

    @Override
    public void onAndroidPayError(int errorCode) {

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
        Log.e(TAG, connectionResult.getErrorMessage());
    }

    //---------------------------------------------
    // Util
    //---------------------------------------------

    void init() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(Constants.WALLET_ENVIRONMENT)
                        .setTheme(WalletConstants.THEME_LIGHT)
                        .build())
                .build();

        mPayButton = (Button) findViewById(R.id.btnPay);
        mPayButton.setEnabled(false);
        mPayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCardToken();
            }
        });

        mCardEditor = (CardEditor) findViewById(R.id.card_editor);
        mCardEditor.addOnStateChangedListener(new CardEditor.OnStateChangedListener() {
            @Override
            public void onStateChange(CardEditor cardEditor) {
                mPayButton.setEnabled(cardEditor.isValid());
            }
        });

        initializeAndroidPay();
    }

    void initializeAndroidPay() {
        Wallet.Payments.isReadyToPay(mGoogleApiClient).setResultCallback(
                new ResultCallback<BooleanResult>() {
                    @Override
                    public void onResult(@NonNull BooleanResult booleanResult) {
                        if (booleanResult.getStatus().isSuccess()) {
                            if (booleanResult.getValue()) {
                                Log.i(TAG, "Android Pay is ready");
                                showGoogleBuyButton();

                                return;
                            }
                        }

                        Log.i(TAG, "Android Pay not ready");
                        hideGoogleBuyButton();
                    }
                });
    }

    void showGoogleBuyButton() {

        findViewById(R.id.buy_button_layout).setVisibility(View.VISIBLE);

        // Define fragment style
        WalletFragmentStyle fragmentStyle = new WalletFragmentStyle()
                .setBuyButtonText(WalletFragmentStyle.BuyButtonText.BUY_WITH)
                .setBuyButtonAppearance(WalletFragmentStyle.BuyButtonAppearance.ANDROID_PAY_DARK)
                .setBuyButtonWidth(WalletFragmentStyle.Dimension.MATCH_PARENT);

        // Define fragment options
        WalletFragmentOptions fragmentOptions = WalletFragmentOptions.newBuilder()
                .setEnvironment(Constants.WALLET_ENVIRONMENT)
                .setFragmentStyle(fragmentStyle)
                .setTheme(WalletConstants.THEME_LIGHT)
                .setMode(WalletFragmentMode.BUY_BUTTON)
                .build();

        // Create a new instance of WalletFragment
        WalletFragment walletFragment = WalletFragment.newInstance(fragmentOptions);

        // Initialize the fragment with start params
        // Note: If using the provided helper method Simplify.handleAndroidPayResult(int, int, Intent),
        //       you MUST set the request code to Simplify.REQUEST_CODE_MASKED_WALLET
        WalletFragmentInitParams startParams = WalletFragmentInitParams.newBuilder()
                .setMaskedWalletRequest(getMaskedWalletRequest())
                .setMaskedWalletRequestCode(Simplify.REQUEST_CODE_MASKED_WALLET)
                .build();

        walletFragment.initialize(startParams);

        // Add Wallet fragment to the UI
        getFragmentManager().beginTransaction()
                .replace(R.id.buy_button_holder, walletFragment, WALLET_FRAGMENT_ID)
                .commit();

    }

    void hideGoogleBuyButton() {
        findViewById(R.id.buy_button_layout).setVisibility(View.GONE);
    }

    void requestCardToken() {

        mPayButton.setEnabled(false);

        Card card = mCardEditor.getCard();

        Simplify.createCardToken(card, new CardToken.Callback() {
            @Override
            public void onSuccess(CardToken cardToken) {
                mPayButton.setEnabled(true);

                Intent i = new Intent(MainActivity.this, ThankYouActivity.class);
                i.putExtra(ThankYouActivity.EXTRA_PAGE, ThankYouActivity.PAGE_SUCCESS);
                startActivity(i);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();

                mPayButton.setEnabled(true);

                Intent i = new Intent(MainActivity.this, ThankYouActivity.class);
                i.putExtra(ThankYouActivity.EXTRA_PAGE, ThankYouActivity.PAGE_FAIL);
                startActivity(i);
            }
        });
    }


    MaskedWalletRequest getMaskedWalletRequest() {

        PaymentMethodTokenizationParameters parameters =
                PaymentMethodTokenizationParameters.newBuilder()
                        .setPaymentMethodTokenizationType(PaymentMethodTokenizationType.NETWORK_TOKEN)
                        .addParameter("publicKey", Simplify.getAndroidPayPublicKey())
                        .build();

        Cart cart = Cart.newBuilder()
                .setCurrencyCode(CURRENCY_CODE_USD)
                .setTotalPrice("15.00")
                .addLineItem(LineItem.newBuilder()
                        .setCurrencyCode(CURRENCY_CODE_USD)
                        .setDescription("Iced Coffee")
                        .setQuantity("1")
                        .setUnitPrice("15.00")
                        .setTotalPrice("15.00")
                        .build())
                .build();

        return MaskedWalletRequest.newBuilder()
                .setMerchantName("Overpriced Coffee Shop")
                .setPhoneNumberRequired(true)
                .setShippingAddressRequired(true)
                .setCurrencyCode(CURRENCY_CODE_USD)
                .setCart(cart)
                .setEstimatedTotalPrice("15.00")
                .setPaymentMethodTokenizationParameters(parameters)
                .build();
    }

}
