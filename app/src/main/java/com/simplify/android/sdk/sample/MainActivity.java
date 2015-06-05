package com.simplify.android.sdk.sample;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.simplify.android.sdk.Simplify;
import com.simplify.android.sdk.model.Card;
import com.simplify.android.sdk.model.SimplifyError;
import com.simplify.android.sdk.model.Token;
import com.simplify.android.sdk.view.CardEditor;

public class MainActivity extends Activity
{
    private static final String TAG = MainActivity.class.getSimpleName();

    private Simplify mSimplify;
    private CardEditor mCardEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSimplify = new Simplify("sbpb_N2ZkOGIwZWYtYTg3My00OTE1LWI3ZjgtMzZhMzZhZTAyYTY5");

        initUI();
    }

    private void initUI()
    {
        mCardEditor = (CardEditor) findViewById(R.id.card_editor);

        mCardEditor.setOnChargeClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                Card card = mCardEditor.getCard();
                AsyncTask<?,?,?> task = mSimplify.createCardToken(card, new Simplify.CreateTokenListener()
                {
                    @Override
                    public void onSuccess(Token token)
                    {
                        Log.i(TAG, "Created Token: " + token.getId());
                        mCardEditor.showSuccessOverlay("Created card token " + token.getId());
                    }

                    @Override
                    public void onError(SimplifyError error)
                    {
                        Log.e(TAG, "Error Creating Token: " + error.getMessage());
                        mCardEditor.showErrorOverlay("Unable to retrieve card token. " + error.getMessage());
                    }
                });
            }
        });

        // init reset button
        findViewById(R.id.btnReset).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mCardEditor.reset();
            }
        });
    }
}
