package com.simplify.android.sdk.sample;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.simplify.android.sdk.CardEditor;
import com.simplify.android.sdk.SimplifyCallback;
import com.simplify.android.sdk.Simplify;
import com.simplify.android.sdk.SimplifyGooglePayCallback;
import com.simplify.android.sdk.SimplifyMap;
import com.simplify.android.sdk.SimplifySecure3DCallback;


public class MainActivity extends AppCompatActivity implements SimplifyGooglePayCallback, SimplifySecure3DCallback {

    static final String TAG = MainActivity.class.getSimpleName();

    PaymentsClient paymentsClient;
    CardEditor cardEditor;
    Button payButton;
    Simplify simplify;
    ProgressBar progressBar;


    //---------------------------------------------
    // Life-Cycle
    //---------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        simplify = ((SimplifyApplication) getApplication()).getSimplify();

        GooglePay.merchantName = "Overpriced Coffee Co.";
        GooglePay.publicKey = simplify.getGooglePayPublicKey();
        GooglePay.currency = Constants.CURRENCY_CODE;

        paymentsClient = Wallet.getPaymentsClient(this, new Wallet.WalletOptions.Builder()
                .setEnvironment(Constants.WALLET_ENVIRONMENT)
                .build());

        TextView amountView = findViewById(R.id.amount);
        amountView.setText(Constants.AMOUNT);

        payButton = findViewById(R.id.btnPay);
        payButton.setEnabled(false);
        payButton.setOnClickListener(v -> manualPayButtonClicked());

        progressBar = findViewById(R.id.progress_bar);

        cardEditor = findViewById(R.id.card_editor);
        cardEditor.addOnStateChangedListener(cardEditor -> payButton.setEnabled(cardEditor.isValid()));

        // click handler on google pay button
        findViewById(R.id.googlepay).setOnClickListener(view -> googlePayButtonClicked());

        isGooglePayReady();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // let the Simplify SDK marshall out the android pay activity results
        if (Simplify.handle3DSResult(requestCode, resultCode, data, this)) {
            return;
        } else if (Simplify.handleGooglePayResult(requestCode, resultCode, data, this)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    //---------------------------------------------
    // Google Pay callback methods
    //---------------------------------------------

    @Override
    public void onReceivedPaymentData(@NonNull PaymentData paymentData) {
        progressBar.setVisibility(View.VISIBLE);
        payButton.setEnabled(false);

        // create a card token
        simplify.createGooglePayCardToken(paymentData, new CardTokenCallback());
    }

    @Override
    public void onGooglePayCancelled() {
        Log.d(TAG, "Google Pay Cancelled");
    }

    @Override
    public void onGooglePayError(@NonNull Status status) {
        Log.d(TAG, "Google Pay Error Code: " + status.getStatusCode());
        Toast.makeText(this, "Google Pay Error Code: " + status.getStatusCode(), Toast.LENGTH_SHORT).show();
    }

    //---------------------------------------------
    // 3DS callback methods
    //---------------------------------------------

    @Override
    public void onSecure3DComplete(boolean success) {
        // TODO - If 3DS authentication was successful, this is where you would send the token ID
        // TODO - and payment information back to your server for processing...

        Toast.makeText(this, "3DS authentication " + (success ? "success" : "failure"), Toast.LENGTH_SHORT).show();

        progressBar.setVisibility(View.GONE);
        payButton.setEnabled(true);

        Intent i = new Intent(MainActivity.this, ThankYouActivity.class);
        i.putExtra(ThankYouActivity.EXTRA_PAGE, success ? ThankYouActivity.PAGE_SUCCESS : ThankYouActivity.PAGE_FAIL);
        startActivity(i);
    }

    @Override
    public void onSecure3DError(String message) {
        Toast.makeText(this, "3DS authentication encountered an error: " + message, Toast.LENGTH_SHORT).show();

        progressBar.setVisibility(View.GONE);
        payButton.setEnabled(true);

        Intent i = new Intent(MainActivity.this, ThankYouActivity.class);
        i.putExtra(ThankYouActivity.EXTRA_PAGE, ThankYouActivity.PAGE_FAIL);
        startActivity(i);
    }

    @Override
    public void onSecure3DCancel() {
        Toast.makeText(this, "3DS authentication cancelled", Toast.LENGTH_SHORT).show();

        progressBar.setVisibility(View.GONE);
        payButton.setEnabled(true);
    }

    //---------------------------------------------
    // Util
    //---------------------------------------------

    void isGooglePayReady() {
        try {
            IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(GooglePay.getIsReadyToPayRequest().toString());
            Task<Boolean> task = paymentsClient.isReadyToPay(request);
            task.addOnCompleteListener(task1 -> {
                try {
                    Boolean result = task1.getResult(ApiException.class);
                    if (result != null && result) {
                        // show Google Pay as a payment option
                        Log.i(TAG, "Google Pay is ready");
                        showGooglePayButton();
                        return;
                    }
                } catch (ApiException e) {
                    // do nothing
                }

                Log.i(TAG, "Google Pay not ready");
                hideGooglePayButton();
            });
        } catch (Exception e) {
            // do nothing
        }
    }

    void showGooglePayButton() {
        findViewById(R.id.googlepay).setVisibility(View.VISIBLE);
    }

    void hideGooglePayButton() {
        findViewById(R.id.googlepay).setVisibility(View.GONE);
    }

    void manualPayButtonClicked() {
        progressBar.setVisibility(View.VISIBLE);
        payButton.setEnabled(false);

        SimplifyMap card = cardEditor.getCard();

        SimplifyMap secure3DRequestData = new SimplifyMap()
                .set("amount", 100)
                .set("currency", Constants.CURRENCY_CODE)
                .set("description", "Iced coffee");

        simplify.createCardToken(card, secure3DRequestData, new CardTokenCallback());
    }

    void googlePayButtonClicked() {
        try {
            String requestJson = GooglePay.getPaymentDataRequest(Constants.AMOUNT).toString();
            PaymentDataRequest request = PaymentDataRequest.fromJson(requestJson);
            Simplify.requestGooglePayData(paymentsClient, request, MainActivity.this);
        } catch (Exception e) {
            // do nothing
        }
    }


    class CardTokenCallback implements SimplifyCallback {
        @Override
        public void onSuccess(@NonNull SimplifyMap response) {
            Toast.makeText(MainActivity.this, "Card token created: " + response.get("id"), Toast.LENGTH_SHORT).show();

            // TODO cache cardToken

            // check if 3DS data present
            if (response.containsKey("card.secure3DData.isEnrolled") && (boolean) response.get("card.secure3DData.isEnrolled")) {
                // start 3DS activity
                Simplify.start3DSActivity(MainActivity.this, response);
                return;
            }


            // TODO - If not performing 3DS authentication, this is where you would send the token ID
            // TODO - and payment information back to your server for processing...

            progressBar.setVisibility(View.GONE);
            payButton.setEnabled(true);

            Intent i = new Intent(MainActivity.this, ThankYouActivity.class);
            i.putExtra(ThankYouActivity.EXTRA_PAGE, ThankYouActivity.PAGE_SUCCESS);
            startActivity(i);
        }

        @Override
        public void onError(@NonNull Throwable throwable) {
            throwable.printStackTrace();

            progressBar.setVisibility(View.GONE);
            payButton.setEnabled(true);

            Intent i = new Intent(MainActivity.this, ThankYouActivity.class);
            i.putExtra(ThankYouActivity.EXTRA_PAGE, ThankYouActivity.PAGE_FAIL);
            startActivity(i);
        }
    }
}
