package com.example.hookapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SPUtils {

    public static final String SP_NAME = "sp";

    public static final String KEY_LOGIN = "sp";

    public static SPUtils instance;

    private static SharedPreferences mSP;

    private SPUtils(Context context){
        mSP = context.getApplicationContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);


    }

    public static SPUtils getInstance(Context context) {
        if(instance == null){
            synchronized (SPUtils.class){
                if(instance == null){
                    instance = new SPUtils(context);
                }
            }
        }
        return instance;
    }

    void login(){
        mSP.edit().putBoolean(KEY_LOGIN,true).apply();
    }

    void logout(){
        mSP.edit().putBoolean(KEY_LOGIN,true).apply();
    }

    boolean isLogin(){
        return mSP.getBoolean(KEY_LOGIN,false);
    }
}
