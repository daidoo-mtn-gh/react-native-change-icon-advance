package com.reactnativechangeicon;

import androidx.annotation.NonNull;

import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.os.Bundle;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

import java.util.ArrayList;
import java.util.List;

@ReactModule(name = "ChangeIcon")
public class ChangeIconModule extends ReactContextBaseJavaModule implements Application.ActivityLifecycleCallbacks {
    public static final String NAME = "ChangeIcon";
    private final String packageName;
    private final List<String> classesToKill = new ArrayList<>();
    private Boolean iconChanged = false;
    private String componentClass = "";
    
    // Add this field to track if we're in the middle of an external activity
    private boolean isInExternalActivity = false;

    public ChangeIconModule(ReactApplicationContext reactContext, String packageName) {
        super(reactContext);
        this.packageName = packageName;
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void resolveEntryPoint(String activityTobeDisabled, Promise promise) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
           promise.reject("ANDROID:ACTIVITY_NOT_FOUND");
            return;
        }

        if(activityTobeDisabled.equals("MainActivity")){
            promise.resolve("Activity already resolved");
            return;
        }

        PackageManager pm = activity.getPackageManager();

        // Enable new activity
        pm.setComponentEnabledSetting(
                new ComponentName(this.packageName, this.packageName + ".MainActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );

        // Disable old activity alias
        pm.setComponentEnabledSetting(
                new ComponentName(this.packageName, this.packageName + "." + activityTobeDisabled),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
        promise.resolve("resolvedEntryPoint: Successful");
    }

    @ReactMethod
    public void getIcon(Promise promise) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.reject("ANDROID:ACTIVITY_NOT_FOUND");
            return;
        }

        final String activityName = activity.getComponentName().getClassName();

        if (activityName.endsWith("MainActivity")) {
            promise.resolve("Default");
            return;
        }
        String[] activityNameSplit = activityName.split("MainActivity");
        if (activityNameSplit.length != 2) {
            promise.reject("ANDROID:UNEXPECTED_COMPONENT_CLASS:" + this.componentClass);
            return;
        }
        promise.resolve(activityNameSplit[1]);
        return;
    }

    @ReactMethod
    public void changeIcon(String iconName, Promise promise) {
        final Activity activity = getCurrentActivity();
        final String activityName = activity.getComponentName().getClassName();
        if (activity == null) {
            promise.reject("ANDROID:ACTIVITY_NOT_FOUND");
            return;
        }
        if (this.componentClass.isEmpty()) {
            this.componentClass = activityName.endsWith("MainActivity") ? activityName + "Default" : activityName;
        }

        final String newIconName = (iconName == null || iconName.isEmpty()) ? "Default" : iconName;
        final String activeClass = this.packageName + ".MainActivity" + newIconName;
        if (this.componentClass.equals(activeClass)) {
            promise.reject("ANDROID:ICON_ALREADY_USED:" + this.componentClass);
            return;
        }

        this.classesToKill.add(this.componentClass);
        this.componentClass = activeClass;
        activity.getApplication().registerActivityLifecycleCallbacks(this);
        iconChanged = true;
        promise.resolve(newIconName);
    }

    private void completeIconChange() {
        if (!iconChanged || isInExternalActivity) return;
        final Activity activity = getCurrentActivity();
        if (activity == null) return;

        activity.getPackageManager().setComponentEnabledSetting(
                new ComponentName(this.packageName, this.componentClass),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        for (String cls : classesToKill) {
            activity.getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this.packageName, cls),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
        classesToKill.clear();
        iconChanged = false;
    }

    @ReactMethod
    public void notifyExternalActivityStarting(Promise promise) {
        // Call this before launching contact picker
        isInExternalActivity = true;
        promise.resolve(null);
    }

    @ReactMethod
    public void notifyExternalActivityFinished(Promise promise) {
        // Call this after returning from contact picker
        isInExternalActivity = false;
        promise.resolve(null);
    }


    @Override
    public void onActivityPaused(Activity activity) {
        completeIconChange();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
      
    }
}
