package com.gae.scaffolder.plugin;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class FCMPluginActivity extends Activity {
    private static String TAG = "FCMPlugin";

    /*
     * this activity will be started if the user touches a notification that we own.
     * We send it's data off to the push plugin for processing.
     * If needed, we boot up the main activity to kickstart the application.
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "==> FCMPluginActivity onCreate");
        handleNotification(this, getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "==> FCMPluginActivity onNewIntent");
        handleNotification(this, intent);
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "==> FCMPluginActivity onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "==> FCMPluginActivity onResume");
        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "==> FCMPluginActivity onStop");
    }

    private static void handleNotification(Context context, Intent intent) {
        try {
            PackageManager packageManager = context.getPackageManager();

            Intent launchIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            Bundle intentExtras = intent.getExtras();
            if (intentExtras == null) {
                return;
            }
            Log.d(TAG, "==> USER TAPPED NOTIFICATION " + intentExtras.toString());
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("wasTapped", true);
            for (String key : intentExtras.keySet()) {
                Object value = intentExtras.get(key);
                Log.d(TAG, "\tKey: " + key + " Value: " + value);
                data.put(key, value);
            }
            FCMPlugin.setInitialPushPayload(data);
            FCMPlugin.sendPushPayload(data);

            launchIntent.putExtras(intentExtras);
            context.startActivity(launchIntent);
        } catch (Exception e) {
            Log.e(TAG, "handleNotification error " + e.getMessage());
        }
    }
}