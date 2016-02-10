package com.simplify.android.sdk.sample;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.simplify.android.sdk.Simplify;

public class SimplifyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // init Simplify SDK with public api key stored in metadata
        try {
            Bundle bundle = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            String apiKey = bundle.getString("com.simplify.android.sdk.apiKey", null);
            if (apiKey != null) {
                // retrieve android pay public key
                String androidPayPublicKey = bundle.getString("com.simplify.android.sdk.androidPayPublicKey", null);

                Simplify.init(apiKey, androidPayPublicKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public GoogleApiClient getGoogleApiClient(Context context) {

        ConnectedCallbacks callbacks = new ConnectedCallbacks();

        return new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(callbacks)
                .addOnConnectionFailedListener(callbacks)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                        .setTheme(WalletConstants.THEME_LIGHT)
                        .build())
                .build();

    }

    static class ConnectedCallbacks implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnected(Bundle bundle) {

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }
    }
}
