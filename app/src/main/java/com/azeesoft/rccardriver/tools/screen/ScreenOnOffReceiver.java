package com.azeesoft.rccardriver.tools.screen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by azizt on 5/20/2017.
 */

public class ScreenOnOffReceiver extends BroadcastReceiver {

    private static String LOG_TAG="ScreenOnOffReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Log.i("LOG_TAG","Screen went OFF");
            ScreenManager.setScreenState(ScreenManager.SCREEN_STATE.OFF);
//            Toast.makeText(context, "screen OFF",Toast.LENGTH_LONG).show();

            /*new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(LOG_TAG,"Waiting to turn on..");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    turnScreenOn(context);
                }
            }).start();*/

        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            Log.i("LOG_TAG","Screen went ON");
            ScreenManager.setScreenState(ScreenManager.SCREEN_STATE.ON);
//            Toast.makeText(context, "screen ON", Toast.LENGTH_LONG).show();
        }
    }
}
