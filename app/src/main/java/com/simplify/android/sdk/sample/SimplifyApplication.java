package com.simplify.android.sdk.sample;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.simplify.android.sdk.Simplify;

public class SimplifyApplication extends Application {

    String mAndroidPayPublicKey;

    @Override
    public void onCreate() {
        super.onCreate();

        // init Simplify SDK with public api key stored in metadata
        try {
            Bundle bundle = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            String apiKey = bundle.getString("com.simplify.android.sdk.apiKey", null);
            if (apiKey != null) {
                Simplify.init(apiKey);
            }

            // get android pay public key
            mAndroidPayPublicKey = bundle.getString("com.simplify.android.sdk.androidPayPublicKey", null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getAndroidPayPublicKey() {
        return mAndroidPayPublicKey;
    }
}
