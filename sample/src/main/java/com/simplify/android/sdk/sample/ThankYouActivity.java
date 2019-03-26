package com.simplify.android.sdk.sample;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ViewFlipper;

public class ThankYouActivity extends AppCompatActivity {

    public static final String EXTRA_PAGE = ThankYouActivity.class.getName() + ".PAGE";

    public static final int PAGE_SUCCESS = 0;
    public static final int PAGE_FAIL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thankyou);

        int page = getIntent().getIntExtra(EXTRA_PAGE, PAGE_SUCCESS);
        ((ViewFlipper) findViewById(R.id.flipper_thankyou)).setDisplayedChild(page);

        findViewById(R.id.text_shop_more).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.text_try_again).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
