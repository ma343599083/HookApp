package com.example.hookapp;

import android.app.Application;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        HookUtils hookUtils = new HookUtils(this);
        hookUtils.hookStartActivity(this);
        hookUtils.hookActivithThreadHandler();

    }
}
