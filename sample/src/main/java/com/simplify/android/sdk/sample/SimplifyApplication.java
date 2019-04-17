package com.simplify.android.sdk.sample;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.simplify.android.sdk.SimplifyKotlin;

public class SimplifyApplication extends Application {

    SimplifyKotlin simplify;


    @Override
    public void onCreate() {
        super.onCreate();

        simplify = new SimplifyKotlin();

        // init Simplify SDK with public api key stored in metadata
        try {
            Bundle bundle = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;

            // init simplify api key
            String apiKey = bundle.getString("com.simplify.android.sdk.apiKey", null);
            if (apiKey != null) {
                simplify.setApiKey(apiKey);
            }

            // init android pay public key
            String androidPayPublicKey = bundle.getString("com.simplify.android.sdk.androidPayPublicKey", null);
            if (androidPayPublicKey != null) {
                simplify.setAndroidPayPublicKey(androidPayPublicKey);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    SimplifyKotlin getSimplify() {
        return simplify;
    }
}
