package com.simplify.android.sdk.sample;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;

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
}
