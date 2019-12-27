package com.example.hookapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class HookUtils {
    private static final String TAG = "HookUtils";

    Context mContext;

    public HookUtils(Context context) {
        this.mContext = context;
    }

    public Resources hookResource(Resources supResource) {
        String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/plugin.apk";
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPathMethod = assetManager.getClass().getDeclaredMethod("addAssetPath",String.class);
            addAssetPathMethod.setAccessible(true);
            addAssetPathMethod.invoke(assetManager, apkPath);
            return new Resources(assetManager, supResource.getDisplayMetrics(), supResource.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
                        boolean needLogin = intent.getBooleanExtra("needLogin", false);
                        if(oldIntent != null){
                            if(!needLogin || SPUtils.getInstance(mContext).isLogin()){
                                intent.setComponent(oldIntent.getComponent());
                            }else {
                                ComponentName componentName = new ComponentName(mContext, LoginActivity.class);
                                intent.setComponent(componentName);
                            }

                            Field activityInfoField = launchActivityItemClazz.getDeclaredField("mInfo");
                            activityInfoField.setAccessible(true);
                            ActivityInfo activityInfo = (ActivityInfo) activityInfoField.get(activityCallback);
                            activityInfo.applicationInfo.packageName = oldIntent.getPackage() == null ? oldIntent.getComponent().getPackageName()
                                    : oldIntent.getPackage();
                            hookPackgeManager();
                        }
                    }

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void hookPackgeManager() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThreadMethod.setAccessible(true);
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);

            Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object sPackageManagerObj = sPackageManagerField.get(currentActivityThread);
            Class<?> iPackageManagerClazz = Class.forName("android.content.pm.IPackageManager");
            Object proxyObj = Proxy.newProxyInstance(
                    iPackageManagerClazz.getClassLoader(),
                    new Class[]{iPackageManagerClazz},
                    new PackageManagerHandler(sPackageManagerObj)
            );
            sPackageManagerField.set(currentActivityThread,proxyObj);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class PackageManagerHandler implements InvocationHandler {
        Object mBase;

        public PackageManagerHandler(Object object) {
            this.mBase = object;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("getPackageInfo")) {
                return new PackageInfo();
            }
            return  method.invoke(mBase,args);
        }
    }

    public void injectPluginClass() {

        //通过DexClassLoader加载插件apk
        String cachePath = mContext.getCacheDir().getAbsolutePath();
        String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/plugin.apk";
        DexClassLoader dexClassLoader = new DexClassLoader(apkPath, cachePath, cachePath, mContext.getClassLoader());
        try {

            //通过反射的手段找到插件apk的Element数组
            Class<?> baseDexClassLoader = Class.forName("dalvik.system.BaseDexClassLoader");
            Field pathListField = baseDexClassLoader.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object pathListObj = pathListField.get(dexClassLoader);
            Field dexElementsField = pathListObj.getClass().getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            Object dexElementsObj = dexElementsField.get(pathListObj);
            int dexElementsLength = Array.getLength(dexElementsObj);

            //通过反射的手段找到宿主apk的Element数组
            PathClassLoader systemClassLoader = (PathClassLoader)mContext.getClassLoader();
            Field systemPathListField = baseDexClassLoader.getDeclaredField("pathList");
            systemPathListField.setAccessible(true);
            Object systemPathListObj = systemPathListField.get(systemClassLoader);
            Field systemDexElementsField = systemPathListObj.getClass().getDeclaredField("dexElements");
            systemDexElementsField.setAccessible(true);
            Object systemDexElementsObj = systemDexElementsField.get(systemPathListObj);
            int systemElementsLength = Array.getLength(systemDexElementsObj);

            //将插件exElements与宿主dexElements融合
            int newLength = dexElementsLength + systemElementsLength;
            Object newDexElementsObj = Array.newInstance(systemDexElementsObj.getClass().getComponentType(), newLength);
            for (int i = 0; i < newLength; i++) {
                if(i < dexElementsLength){
                    Array.set(newDexElementsObj,i,Array.get(dexElementsObj,i));
                }else {
                    Array.set(newDexElementsObj,i,Array.get(systemDexElementsObj,i-dexElementsLength));
                }
            }

            //将新的dexElements设置到宿主中
            systemDexElementsField.set(systemPathListObj,newDexElementsObj);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //每一个apk都对应一个LoadedApk
    //1.修改ActivityThread的mPackages
    //2.根据apk生成LoadedApk
    //3.将生成的LoadedApk加入到mPackages
    public void putLoadedApk(){
        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/plugin.apk";

            //拿到系统ActivityThread对象
            Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = activityThreadClazz.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object sCurrentActivityThreadObj = sCurrentActivityThreadField.get(null);

            //获取ActivityThread中的LoadedApk集合 字段名：mPackages
            Field mPackagesField = activityThreadClazz.getDeclaredField("mPackages");
            mPackagesField.setAccessible(true);
            Map mPackages = (Map) mPackagesField.get(sCurrentActivityThreadObj);

            ApplicationInfo applicationInfo = getApplicationInfo(path);

            //生成LoadedApk
            Object loadedApkObj = generateLoadedApk(path,applicationInfo, activityThreadClazz, sCurrentActivityThreadObj);

            Field mClassLoaderField = loadedApkObj.getClass().getDeclaredField("mClassLoader");
            mClassLoaderField.setAccessible(true);
            String pluginOptDexDirPath = Utils.getPluginOptDexDir(applicationInfo.packageName).getPath();
            String pluginLibDirPath = Utils.getPluginLibDir(applicationInfo.packageName).getPath();
            mClassLoaderField.set(loadedApkObj,new CustomClassLoader(path,pluginOptDexDirPath,pluginLibDirPath,mContext.getClassLoader()));

            //将LoadedApk放入map中
            WeakReference<Object> objectWeakReference = new WeakReference<Object>(loadedApkObj);

            mPackages.put(applicationInfo.packageName,objectWeakReference);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object generateLoadedApk(String path,ApplicationInfo applicationInfo,Class activityThreadClazz,Object sCurrentActivityThreadObj) {
        try {
            Class<?> CompatibilityInfoClazz = Class.forName("android.content.res.CompatibilityInfo");
            Method getPackageInfoNoCheckMethod = activityThreadClazz.getDeclaredMethod("getPackageInfoNoCheck", ApplicationInfo.class, CompatibilityInfoClazz);
            getPackageInfoNoCheckMethod.setAccessible(true);
            //参数    ApplicationInfo ai,CompatibilityInfo compatInfo

            Field defaultCompatibilityInfoField = CompatibilityInfoClazz.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
            Object defaultCompatibilityInfoObj = defaultCompatibilityInfoField.get(null);

            Object loadedApkObj = getPackageInfoNoCheckMethod.invoke(sCurrentActivityThreadObj, applicationInfo, defaultCompatibilityInfoObj);
            return loadedApkObj;
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }


    private ApplicationInfo getApplicationInfo(String path) {
        try {
            Class packageParserClass = Class.forName("android.content.pm.PackageParser");
            Method parsePackageMethod = packageParserClass.getDeclaredMethod("parsePackage", File.class, int.class);
            Object packageParser = packageParserClass.newInstance();
            Object packageObj = parsePackageMethod.invoke(packageParser, new File(path), PackageManager.GET_ACTIVITIES);

            // 调用generateActivityInfo 方法, 把PackageParser.Activity 转换成
            Class<?> packageParser$ActivityClass = Class.forName("android.content.pm.PackageParser$Activity");
//            generateActivityInfo方法
            Class<?> packageUserStateClass = Class.forName("android.content.pm.PackageUserState");
            Object defaltUserState = packageUserStateClass.newInstance();
            Method generateReceiverInfo = packageParserClass.getDeclaredMethod("generateActivityInfo",
                    packageParser$ActivityClass, int.class, packageUserStateClass, int.class);
            Class<?> userHandler = Class.forName("android.os.UserHandle");
            Method getCallingUserIdMethod = userHandler.getDeclaredMethod("getCallingUserId");
            int userId = (int) getCallingUserIdMethod.invoke(null);
//目的     generateApplicationInfo  方法  生成  ApplicationInfo
            // 需要调用 android.content.pm.PackageParser#generateActivityInfo(android.content.pm.ActivityInfo, int, android.content.pm.PackageUserState, int)
            //      generateApplicationInfo
            Method generateApplicationInfoMethod = packageParserClass.getDeclaredMethod("generateApplicationInfo",
                    packageObj.getClass(),
                    int.class,
                    packageUserStateClass);
            ApplicationInfo applicationInfo = (ApplicationInfo) generateApplicationInfoMethod.invoke(packageParser, packageObj, 0, defaltUserState);

            applicationInfo.sourceDir = path;
            applicationInfo.publicSourceDir = path;
            return applicationInfo;
            //generateActivityInfo
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}
