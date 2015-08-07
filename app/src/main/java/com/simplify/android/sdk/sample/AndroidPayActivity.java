package com.simplify.android.sdk.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.MaskedWallet;
import com.simplify.android.sdk.Simplify;

public class AndroidPayActivity extends Activity implements Simplify.WalletCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_pay);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (Simplify.handleWalletResult(requestCode, resultCode, data, this)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onReceivedMaskedWallet(MaskedWallet maskedWallet) {

    }

    @Override
    public void onReceivedFullWallet(FullWallet fullWallet) {

    }

    @Override
    public void onWalletCancelled() {

    }

    @Override
    public void onWalletError(int errorCode) {

    }
}
