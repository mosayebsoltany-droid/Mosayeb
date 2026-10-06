package com.mose.assistant;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.content.ContextCompat;

public class MoseApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private boolean started;

    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override public void onActivityResumed(Activity activity) {
        if (!started) {
            started = true;
            try {
                ContextCompat.startForegroundService(this, new Intent(this, WakeWordService.class));
            } catch (Exception ignored) { }
        }
    }

    @Override public void onActivityCreated(Activity a, Bundle b) {}
    @Override public void onActivityStarted(Activity a) {}
    @Override public void onActivityPaused(Activity a) {}
    @Override public void onActivityStopped(Activity a) {}
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
    @Override public void onActivityDestroyed(Activity a) {}
}
