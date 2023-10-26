package com.gae.scaffolder.plugin;

import androidx.core.app.NotificationManagerCompat;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.gae.scaffolder.plugin.interfaces.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class FCMPlugin extends CordovaPlugin {
    public static String notificationEventName = "notification";
    public static String tokenRefreshEventName = "tokenRefresh";
    public static Map<String, Object> initialPushPayload;
    public static final String TAG = "FCMPlugin";
    private static FCMPlugin instance;
    protected Context context;
    protected static CallbackContext jsEventBridgeCallbackContext;
    protected static final String POST_NOTIFICATIONS = "POST_NOTIFICATIONS";
    protected static final int POST_NOTIFICATIONS_PERMISSION_REQUEST_ID = 1;

    private static CallbackContext postNotificationPermissionRequestCallbackContext;

    public FCMPlugin() {}
    public FCMPlugin(Context context) {
        this.context = context;
    }

    public static synchronized FCMPlugin getInstance(Context context) {
        if (instance == null) {
            instance = new FCMPlugin(context);
            instance = getPlugin(instance);
        }

        return instance;
    }

    public static synchronized FCMPlugin getInstance() {
        if (instance == null) {
            instance = new FCMPlugin();
            instance = getPlugin(instance);
        }

        return instance;
    }

    public static FCMPlugin getPlugin(FCMPlugin plugin) {
        if (plugin.webView != null) {
            instance = (FCMPlugin) plugin.webView.getPluginManager().getPlugin(FCMPlugin.class.getName());
        } else {
            plugin.initialize(null, null);
            instance = plugin;
        }

        return instance;
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.d(TAG, "==> FCMPlugin initialize");

        FirebaseMessaging.getInstance().subscribeToTopic("android");
        FirebaseMessaging.getInstance().subscribeToTopic("all");
    }

    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "==> FCMPlugin execute: " + action);

        try {
            if (action.equals("ready")) {
                callbackContext.success();
            } else if (action.equals("startJsEventBridge")) {
                this.jsEventBridgeCallbackContext = callbackContext;
            } else if (action.equals("getToken")) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        getToken(callbackContext);
                    }
                });
            } else if (action.equals("getInitialPushPayload")) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        getInitialPushPayload(callbackContext);
                    }
                });
            } else if (action.equals("subscribeToTopic")) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            FirebaseMessaging.getInstance().subscribeToTopic(args.getString(0));
                            callbackContext.success();
                        } catch (Exception e) {
                            callbackContext.error(e.getMessage());
                        }
                    }
                });
            } else if (action.equals("unsubscribeFromTopic")) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic(args.getString(0));
                            callbackContext.success();
                        } catch (Exception e) {
                            callbackContext.error(e.getMessage());
                        }
                    }
                });
            } else if (action.equals("clearAllNotifications")) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            Context context = cordova.getActivity();
                            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.cancelAll();
                            callbackContext.success();
                        } catch (Exception e) {
                            callbackContext.error(e.getMessage());
                        }
                    }
                });
            } else if (action.equals("createNotificationChannel")) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        new FCMPluginChannelCreator(getContext()).createNotificationChannel(callbackContext, args);
                    }
                });
            } else if (action.equals("deleteInstanceId")) {
                this.deleteInstanceId(callbackContext);
            } else if (action.equals("hasPermission")) {
                this.hasPermission(callbackContext);
            } else if (action.equals("requestPushPermission")) {
                this.grantPermission(callbackContext);
            } else {
                callbackContext.error("Method not found");
                return false;
            }
        } catch (Exception e) {
            Log.d(TAG, "ERROR: onPluginAction: " + e.getMessage());
            callbackContext.error(e.getMessage());
            return false;
        }

        return true;
    }

    public void getInitialPushPayload(CallbackContext callback) {
        if(initialPushPayload == null) {
            Log.d(TAG, "getInitialPushPayload: null");
            callback.success((String) null);
            return;
        }
        Log.d(TAG, "getInitialPushPayload");
        try {
            JSONObject jo = new JSONObject();
            for (String key : initialPushPayload.keySet()) {
                jo.put(key, initialPushPayload.get(key));
                Log.d(TAG, "\tinitialPushPayload: " + key + " => " + initialPushPayload.get(key));
            }
            callback.success(jo);
        } catch(Exception error) {
            try {
                callback.error(exceptionToJson(error));
            }
            catch (JSONException jsonErr) {
                Log.e(TAG, "Error when parsing json", jsonErr);
            }
        }
    }

    public void getToken(final TokenListeners<String, JSONObject> callback) {
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(Task<String> task) {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "getInstanceId failed", task.getException());
                        try {
                            callback.error(exceptionToJson(task.getException()));
                        }
                        catch (JSONException jsonErr) {
                            Log.e(TAG, "Error when parsing json", jsonErr);
                        }
                        return;
                    }

                    // Get new Instance ID token
                    String newToken = task.getResult();

                    Log.i(TAG, "\tToken: " + newToken);
                    callback.success(newToken);
                }
            });

            FirebaseMessaging.getInstance().getToken().addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(final Exception e) {
                    try {
                        Log.e(TAG, "Error retrieving token: ", e);
                        callback.error(exceptionToJson(e));
                    } catch (JSONException jsonErr) {
                        Log.e(TAG, "Error when parsing json", jsonErr);
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "\tError retrieving token", e);
            try {
                callback.error(exceptionToJson(e));
            } catch(JSONException je) {}
        }
    }

    private void deleteInstanceId(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebaseMessaging.getInstance().deleteToken();
                    callbackContext.success();
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void hasPermission(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    NotificationManagerCompat notificationManagerCompat =
                            NotificationManagerCompat.from(cordova.getActivity().getApplicationContext());

                    boolean areNotificationsEnabled = notificationManagerCompat.areNotificationsEnabled();

                    boolean hasRuntimePermission;

                    if (Build.VERSION.SDK_INT >= 33) { // Android 13+
                        hasRuntimePermission = hasRuntimePermission(POST_NOTIFICATIONS);
                    } else {
                        hasRuntimePermission = true;
                    }

                    callbackContext.success(areNotificationsEnabled && hasRuntimePermission ? 1 : 0);
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void grantPermission(final CallbackContext callbackContext) {
        CordovaPlugin plugin = this;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= 33) { // Android 13+
                        boolean hasRuntimePermission = hasRuntimePermission(POST_NOTIFICATIONS);
                        if (hasRuntimePermission) {
                            callbackContext.success(1);
                        }
                        else {
                            String[] permissions = new String[]{qualifyPermission(POST_NOTIFICATIONS)};
                            postNotificationPermissionRequestCallbackContext = callbackContext;
                            requestPermissions(plugin, POST_NOTIFICATIONS_PERMISSION_REQUEST_ID, permissions);
                            sendEmptyPluginResultAndKeepCallback(callbackContext);
                        }
                    } else {
                        callbackContext.success(1);
                    }

                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    protected void sendEmptyPluginResultAndKeepCallback(CallbackContext callbackContext) {
        PluginResult pluginresult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginresult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginresult);
    }

    protected String qualifyPermission(String permission) {
        if (permission.startsWith("android.permission.")) {
            return permission;
        } else {
            return "android.permission." + permission;
        }
    }

    protected boolean hasRuntimePermission(String permission) throws Exception {
        boolean hasRuntimePermission = true;
        String qualifiedPermission = qualifyPermission(permission);
        java.lang.reflect.Method method = null;
        try {
            method = cordova.getClass().getMethod("hasPermission", qualifiedPermission.getClass());
            Boolean bool = (Boolean) method.invoke(cordova, qualifiedPermission);
            hasRuntimePermission = bool.booleanValue();
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "Cordova v" + CordovaWebView.CORDOVA_VERSION + " does not support runtime permissions so defaulting to GRANTED for " + permission);
        }
        return hasRuntimePermission;
    }

    protected void requestPermissions(CordovaPlugin plugin, int requestCode, String[] permissions) throws Exception {
        try {
            java.lang.reflect.Method method = cordova.getClass().getMethod("requestPermissions", org.apache.cordova.CordovaPlugin.class, int.class, java.lang.String[].class);
            method.invoke(cordova, plugin, requestCode, permissions);
        } catch (NoSuchMethodException e) {
            throw new Exception("requestPermissions() method not found in CordovaInterface implementation of Cordova v" + CordovaWebView.CORDOVA_VERSION);
        }
    }

    /************
     * Overrides
     ***********/

    /**
     * then updates the list of status based on the grantResults before passing the result back via the context.
     *
     * @param requestCode  - ID that was used when requesting permissions
     * @param permissions  - list of permissions that were requested
     * @param grantResults - list of flags indicating if above permissions were granted or denied
     */
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        String sRequestId = String.valueOf(requestCode);
        Log.v(TAG, "Received result for permissions request id=" + sRequestId);
        try {
            if (postNotificationPermissionRequestCallbackContext == null) {
                Log.e(TAG, "No callback context found for permissions request id=" + sRequestId);
                return;
            }

            boolean postNotificationPermissionGranted = false;
            for (int i = 0, len = permissions.length; i < len; i++) {
                String androidPermission = permissions[i];

                if (androidPermission.equals(qualifyPermission(POST_NOTIFICATIONS))) {
                    postNotificationPermissionGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }

            postNotificationPermissionRequestCallbackContext.success(postNotificationPermissionGranted ? 1 : 0);
            postNotificationPermissionRequestCallbackContext = null;

        } catch (Exception e) {
            if (postNotificationPermissionRequestCallbackContext != null) {
                postNotificationPermissionRequestCallbackContext.error(e.getMessage());
            } else {
                Log.e(TAG, "onRequestPermissionResult error " + e.getMessage());
            }
        }
    }

    private JSONObject exceptionToJson(final Exception exception) throws JSONException {
        return new JSONObject() {
            {
                put("message", exception.getMessage());
                put("cause", exception.getClass().getName());
                put("stacktrace", exception.getStackTrace().toString());
            }
        };
    }

    public void getToken(final CallbackContext callbackContext) {
        this.getToken(new TokenListeners<String, JSONObject>() {
            @Override
            public void success(String message) {
                callbackContext.success(message);
            }

            @Override
            public void error(JSONObject message) {
                callbackContext.error(message);
            }
        });
    }

    private static void dispatchJSEvent(String eventName, String stringifiedJSONValue) throws Exception {
        String jsEventData = "[\"" + eventName + "\"," + stringifiedJSONValue + "]";
        PluginResult dataResult = new PluginResult(PluginResult.Status.OK, jsEventData);
        dataResult.setKeepCallback(true);
        if(FCMPlugin.jsEventBridgeCallbackContext == null) {
            Log.d(TAG, "\tUnable to send event due to unreachable bridge context");
            return;
        }
        FCMPlugin.jsEventBridgeCallbackContext.sendPluginResult(dataResult);
        Log.d(TAG, "\tSent event: " + eventName + " with " + stringifiedJSONValue);
    }

    public static void setInitialPushPayload(Map<String, Object> payload) {
        if(initialPushPayload == null) {
            initialPushPayload = payload;
        }
    }

    public static void sendPushPayload(Map<String, Object> payload) {
        Log.d(TAG, "==> FCMPlugin sendPushPayload");
        try {
            JSONObject jo = new JSONObject();
            for (String key : payload.keySet()) {
                jo.put(key, payload.get(key));
                Log.d(TAG, "\tpayload: " + key + " => " + payload.get(key));
            }
            FCMPlugin.dispatchJSEvent(notificationEventName, jo.toString());
        } catch (Exception e) {
            Log.d(TAG, "\tERROR sendPushPayload: " + e.getMessage());
        }
    }

    public static void sendTokenRefresh(String token) {
        Log.d(TAG, "==> FCMPlugin sendTokenRefresh");
        try {
            FCMPlugin.dispatchJSEvent(tokenRefreshEventName, "\"" + token + "\"");
        } catch (Exception e) {
            Log.d(TAG, "\tERROR sendTokenRefresh: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        initialPushPayload = null;
        jsEventBridgeCallbackContext = null;
    }

    protected Context getContext() {
        context = cordova != null ? cordova.getActivity().getBaseContext() : context;
        if (context == null) {
            throw new RuntimeException("The Android Context is required. Verify if the 'activity' or 'context' are passed by constructor");
        }

        return context;
    }
}
