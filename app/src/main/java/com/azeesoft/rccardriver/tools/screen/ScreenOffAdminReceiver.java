package com.azeesoft.rccardriver.tools.screen;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.azeesoft.rccardriver.R;

/**
 * Created by azizt on 5/20/2017.
 */

public class ScreenOffAdminReceiver extends DeviceAdminReceiver {
    private void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context,
                "Admin receiver status: Enabled");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context,
                "Admin receiver status: Disabled");
    }

}