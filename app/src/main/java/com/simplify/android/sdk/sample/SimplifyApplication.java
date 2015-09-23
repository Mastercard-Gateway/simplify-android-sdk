package com.simplify.android.sdk.sample;

import android.app.Application;

import com.simplify.android.sdk.Simplify;

public class SimplifyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Simplify.init("sbpb_N2ZkOGIwZWYtYTg3My00OTE1LWI3ZjgtMzZhMzZhZTAyYTY5");
    }
}
