package com.example.hookapp;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import java.lang.reflect.Method;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static Context  instance;
    private AssetManager assetManager;
    private Resources newResource;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        HookUtils hookUtils = new HookUtils(this);
        hookUtils.hookStartActivity(this);
        hookUtils.hookActivithThreadHandler();
//        hookUtils.injectPluginClass();
        hookUtils.putLoadedApk();
        newResource = hookUtils.hookResource(getResources());
        if(newResource != null){
            assetManager = newResource.getAssets();
        }

    }

    public static Context getInstance() {
        return instance;
    }


    public AssetManager getAssetManager() {
        return assetManager == null ? super.getAssets() : assetManager;
    }

    @Override
    public Resources getResources() {
        return newResource == null ? super.getResources() : newResource;
    }
}
