package com.simplify.android.sdk.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class ThankYouActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thankyou);

        TextView tv = (TextView) findViewById(R.id.text_shop_more);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shopMore();
            }
        });
    }

    void shopMore() {

    }
}
