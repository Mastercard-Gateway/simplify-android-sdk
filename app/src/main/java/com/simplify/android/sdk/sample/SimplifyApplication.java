package com.simplify.android.sdk.sample;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.simplify.android.sdk.Simplify;

public class SimplifyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // init Simplify SDK with public key stored in metadata
        try {
            Bundle bundle = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            String apiKey = bundle.getString("com.simplify.android.sdk.apikey");
            if (apiKey != null) {
                Simplify.init(apiKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
