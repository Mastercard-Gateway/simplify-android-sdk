package com.simplify.android.sdk.sample;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.simplify.android.sdk.CardEditor;
import com.simplify.android.sdk.SimplifyCallback;
import com.simplify.android.sdk.Simplify;
import com.simplify.android.sdk.SimplifyMap;
import com.simplify.android.sdk.SimplifySecure3DCallback;


public class MainActivity extends AppCompatActivity implements SimplifySecure3DCallback {

    static final String AMOUNT_TEXT = "1.00";
    static final long AMOUNT_VALUE = 100;
    static final String CURRENCY_CODE = "AUD";

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

        // Instantiate the Simplify object with your api key
        simplify = new Simplify(BuildConfig.API_KEY);

        TextView amountView = findViewById(R.id.amount);
        amountView.setText(AMOUNT_TEXT);

        payButton = findViewById(R.id.btnPay);
        payButton.setEnabled(false);
        payButton.setOnClickListener(v -> payButtonClicked());

        progressBar = findViewById(R.id.progress_bar);

        cardEditor = findViewById(R.id.card_editor);
        cardEditor.addOnStateChangedListener(cardEditor -> payButton.setEnabled(cardEditor.isValid()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // let the Simplify SDK marshall out the android pay activity results
        if (Simplify.handle3DSResult(requestCode, resultCode, data, this)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
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
    public void onSecure3DError(@NonNull String message) {
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

    void payButtonClicked() {
        progressBar.setVisibility(View.VISIBLE);
        payButton.setEnabled(false);

        SimplifyMap card = cardEditor.getCard();

        SimplifyMap secure3DRequestData = new SimplifyMap()
                .set("amount", AMOUNT_VALUE)
                .set("currency", CURRENCY_CODE)
                .set("description", "Iced coffee");

        simplify.createCardToken(card, secure3DRequestData, new CardTokenCallback());
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
