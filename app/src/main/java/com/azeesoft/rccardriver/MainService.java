package com.azeesoft.rccardriver;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.azeesoft.rccardriver.tools.bluetooth.RCBluetoothMaster;
import com.azeesoft.rccardriver.tools.screen.ScreenOnOffReceiver;
import com.azeesoft.rccardriver.tools.wifi.CommConstants;
import com.azeesoft.rccardriver.tools.wifi.IPServer;

import org.json.JSONException;
import org.json.JSONObject;

public class MainService extends Service {


    enum SERVICE_INTENT_EXTRAS {RESET_CONNECTIONS, CONNECT_TO_RC_CAR, START_WIFI_SERVER}

    public final static String SERVICE_INTENT_ACTION = "com.azeesoft.rccardriver.action.SERVICE_INTENT_ACTION";
    public final static String SERVICE_INTENT_EXTRA_NAME = "com.azeesoft.rccardriver.extra.SERVICE_INTENT_EXTRA_NAME";


    private static String LOG_TAG = "MainService";

    private ScreenOnOffReceiver screenOnOffReceiver = new ScreenOnOffReceiver();

    private IPServer ipServer;
    private RCBluetoothMaster bluetoothMaster;

    private static MainService thisService;

    public MainService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "Service Created!");

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("RC Driver Running")
                .setContentText("AzeeTech")
                .setContentIntent(pendingIntent).build();

        startForeground(1004, notification);
        thisService = this;

        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOnOffReceiver, screenStateFilter);

        initiateWifiServer();
        bluetoothMaster = new RCBluetoothMaster();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static void executeAction(Intent intent){
        thisService.handleIntent(intent);
    }

    public void handleIntent(Intent intent) {
        if(intent!=null){
            String action = intent.getAction();
            if(action!=null && action.equals(SERVICE_INTENT_ACTION)){
                SERVICE_INTENT_EXTRAS action_extra = (SERVICE_INTENT_EXTRAS) intent.getSerializableExtra(SERVICE_INTENT_EXTRA_NAME);
                switch(action_extra){
                    case RESET_CONNECTIONS:
                        resetAllWifiConnections();
                        break;
                    case CONNECT_TO_RC_CAR:
                        connectToRCCar();
                        break;
                    case START_WIFI_SERVER:
                        startWifiServer();
                        break;
                    default:

                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenOnOffReceiver);
    }

    private void initiateWifiServer() {
        ipServer = IPServer.getIPServer(IPServer.DEFAULT_PORT, 1);
        ipServer.startServer(new IPServer.OnClientDataReceivedListener() {
            @Override
            public void onClientDataReceived(IPServer.ClientConnection clientConnection, JSONObject jsonObject) {
//                Toast.makeText(MainService.this, "Data Received: " + jsonObject, Toast.LENGTH_LONG).show();

                //ipServer.sendDataToAll("Returning data - " + data);
                processWifiData(clientConnection, jsonObject);
            }
        });
    }

    public void processWifiData(IPServer.ClientConnection clientConnection, JSONObject jsonObject){
        try {
            if(jsonObject.getBoolean(CommConstants.RESPONSE_NAME_SUCCESS)){
                if(jsonObject.has(CommConstants.RESPONSE_NAME_ARDUINO_BLUETOOTH_DATA)){
                    if(bluetoothMaster!=null){
                        bluetoothMaster.sendData(jsonObject.getString(CommConstants.RESPONSE_NAME_ARDUINO_BLUETOOTH_DATA));
                    }
                }
                if(jsonObject.has(CommConstants.RESPONSE_NAME_CONNECT_TO_RC_CAR)){
                    if(bluetoothMaster!=null){
                        bluetoothMaster.connectToRCCar();
                    }
                }
                if(jsonObject.has(CommConstants.RESPONSE_NAME_DISCONNECT_FROM_RC_CAR)){
                    if(bluetoothMaster!=null){
                        bluetoothMaster.closeAllConnections();
                    }
                }
                if(jsonObject.has(CommConstants.RESPONSE_NAME_RESET_WIFI_CONNECTIONS)){
                    resetAllWifiConnections();
                }
                if(jsonObject.has(CommConstants.RESPONSE_NAME_START_WIFI_SERVER)){
                    startWifiServer();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendSimpleBroadcastToActivity(String param) {
        Intent intent = new Intent(MainActivity.ACTIVITY_INTENT_ACTION);
        intent.putExtra(MainActivity.ACTIVITY_INTENT_EXTRA_NAME, param);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.sendBroadcast(intent);
    }


    /*
    * Service Actions
    * */

    public void resetAllWifiConnections(){
        if(ipServer!=null) {
            ipServer.resetAllConnections();
        }
    }

    public void connectToRCCar(){
        if(bluetoothMaster!=null){
            bluetoothMaster.connectToRCCar();
        }
    }

    public void startWifiServer(){
        if(ipServer!=null){
            ipServer.stopServer();
        }
        initiateWifiServer();
    }

}
