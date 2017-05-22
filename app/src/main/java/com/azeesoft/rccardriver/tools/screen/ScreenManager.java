package com.azeesoft.rccardriver.tools.screen;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by azizt on 5/20/2017.
 */

public class ScreenManager {

    enum SCREEN_STATE{ON, OFF}

    private static String LOG_TAG="ScreenManager";

    static SCREEN_STATE ScreenState = SCREEN_STATE.ON;


    public static SCREEN_STATE getScreenState() {
        return ScreenState;
    }

    public static void setScreenState(SCREEN_STATE screenState) {
        ScreenState = screenState;
    }

    public static void turnScreenOn(Context context){
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
        wakeLock.acquire();
    }

    public static void turnScreenOff(final Context context) {
        DevicePolicyManager policyManager = (DevicePolicyManager) context
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminReceiver = new ComponentName(context,
                ScreenOffAdminReceiver.class);
        boolean admin = policyManager.isAdminActive(adminReceiver);
        if (admin) {
            Log.i(LOG_TAG, "Going to sleep now.");
            policyManager.lockNow();
        } else {
            Log.i(LOG_TAG, "Not an admin");
            Toast.makeText(context, "Device Admin not enabled",
                    Toast.LENGTH_LONG).show();
        }
    }
}
