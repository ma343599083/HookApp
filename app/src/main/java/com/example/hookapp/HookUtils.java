package com.example.hookapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class HookUtils {
    private static final String TAG = "HookUtils";

    Context mContext;

    public HookUtils(Context context) {
        this.mContext = context;
    }

    class StartActivityHandler implements InvocationHandler{

        Object iActivityManagerObject;

        public StartActivityHandler(Object iActivityManagerObject) {
            this.iActivityManagerObject = iActivityManagerObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.i(TAG,"invoke    "+method.getName());
            if("startActivity".equals(method.getName())){
                if(args != null && args.length>0){
                    Object arg = null;
                    for (int i = 0; i < args.length; i++) {
                        arg = args[i];
                        if(arg instanceof Intent){
                            Intent intent = (Intent) arg;
                            Intent newIntent = new Intent();
                            ComponentName componentName = new ComponentName(mContext, ProxyActivity.class);
                            newIntent.setComponent(componentName);
                            // 真实的意图 隐藏到了  键值对
                            newIntent.putExtra("oldIntent", intent);
                            args[i] = newIntent;
                        }
                    }
                }
            }
            return method.invoke(iActivityManagerObject, args);
        }
    }
    public void hookStartActivity(Context context){
        try {
            Class<?> activityManager = Class.forName("android.app.ActivityManager");
            Field iActivityManagerSingletonField = activityManager.getDeclaredField("IActivityManagerSingleton");
            iActivityManagerSingletonField.setAccessible(true);
            Object iActivityManagerSingleton = iActivityManagerSingletonField.get(null);
            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = singletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            Object IActivityManager = mInstanceField.get(iActivityManagerSingleton);

            Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
            Object newActivityManagerObj = Proxy.newProxyInstance(
                    context.getClassLoader(),
                    new Class[]{iActivityManagerClass},
                    new StartActivityHandler(IActivityManager)
            );
            mInstanceField.set(iActivityManagerSingleton,newActivityManagerObj);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void hookActivithThreadHandler(){
        try {
            Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = activityThreadClazz.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object activityThread = sCurrentActivityThreadField.get(null);
            Field mHField = activityThreadClazz.getDeclaredField("mH");
            mHField.setAccessible(true);
            Handler handler = (Handler) mHField.get(activityThread);
            Field mCallback = Handler.class.getDeclaredField("mCallback");
            mCallback.setAccessible(true);
            mCallback.set(handler,new MyHandlerCallback(handler));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    class MyHandlerCallback implements Handler.Callback{

        private Handler mH;

        public MyHandlerCallback(Handler mH) {
            this.mH = mH;
        }

        @Override
        public boolean handleMessage(Message msg) {
            Log.i(TAG, "handleMessage() called with: msg = [" + msg.toString() + "]");
            replaceContent(msg);
            mH.handleMessage(msg);
            return true;
        }

        private void replaceContent(Message msg) {
            Object obj = msg.obj;
            try {
                Class<?> clientTransactionClazz = Class.forName("android.app.servertransaction.ClientTransaction");
                Field mActivityCallbacksField = clientTransactionClazz.getDeclaredField("mActivityCallbacks");
                mActivityCallbacksField.setAccessible(true);
                List mActivityCallbacks = (List)mActivityCallbacksField.get(obj);
                if(mActivityCallbacks != null && mActivityCallbacks.size()>0){
                    Object activityCallback = mActivityCallbacks.get(0);
                    Class<?> launchActivityItemClazz = Class.forName("android.app.servertransaction.LaunchActivityItem");
                    if(launchActivityItemClazz.isInstance(activityCallback)){
                        Field mIntentField = launchActivityItemClazz.getDeclaredField("mIntent");
                        mIntentField.setAccessible(true);
                        Intent intent = (Intent) mIntentField.get(activityCallback);
                        Intent oldIntent = intent.getParcelableExtra("oldIntent");
                        if(oldIntent != null){
                            if(SPUtils.getInstance(mContext).isLogin()){
                                intent.setComponent(oldIntent.getComponent());
                            }else {
                                ComponentName componentName = new ComponentName(mContext, LoginActivity.class);
                                intent.setComponent(componentName);
                            }
                        }
                    }

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
