package com.azeesoft.rccardriver;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.SurfaceView;

import com.azeesoft.rccardriver.tools.bluetooth.RCBluetoothMaster;
import com.azeesoft.rccardriver.tools.hls.CameraStreamer;
import com.azeesoft.rccardriver.tools.screen.ScreenOnOffReceiver;
import com.azeesoft.rccardriver.tools.wifi.CommConstants;
import com.azeesoft.rccardriver.tools.wifi.IPServer;
import com.azeesoft.rccardriver.tools.wifi.MicStreamer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InvalidObjectException;
import java.util.Locale;

public class MainService extends Service {

    public enum SERVICE_INTENT_EXTRAS {RESET_CONNECTIONS, CONNECT_TO_RC_CAR, START_WIFI_SERVER, START_HLS_SERVER, STOP_HLS_SERVER}

    public final static String SELF_NAME = "Zerone";

    public final static String SERVICE_INTENT_ACTION = "com.azeesoft.rccardriver.action.SERVICE_INTENT_ACTION";
    public final static String SERVICE_INTENT_EXTRA_NAME = "com.azeesoft.rccardriver.extra.SERVICE_INTENT_EXTRA_NAME";

    private static String LOG_TAG = "MainService";

    private ScreenOnOffReceiver screenOnOffReceiver = new ScreenOnOffReceiver();

    private IPServer ipServer;
    private RCBluetoothMaster bluetoothMaster;

    private static MainService thisService;

    private TextToSpeech textToSpeech;

    CameraStreamer cameraStreamer;
    MicStreamer micStreamer;

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

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    Log.d(LOG_TAG, "TTS Initialized!");
                    textToSpeech.setLanguage(Locale.US);
                    textToSpeech.setSpeechRate(1.0f);
                    speak("Initializing System... You are ready to connect!", TextToSpeech.QUEUE_FLUSH);
                }
            }
        });

        cameraStreamer = new CameraStreamer(Camera.CameraInfo.CAMERA_FACING_FRONT,false, 8080, 0, 60, new SurfaceView(this).getHolder());
        micStreamer = MicStreamer.getMicStreamer("");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static void executeAction(Intent intent) {
        if(thisService!=null) {
            thisService.handleIntent(intent);
        }
    }

    public void handleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equals(SERVICE_INTENT_ACTION)) {
                SERVICE_INTENT_EXTRAS action_extra = (SERVICE_INTENT_EXTRAS) intent.getSerializableExtra(SERVICE_INTENT_EXTRA_NAME);
                switch (action_extra) {
                    case RESET_CONNECTIONS:
                        resetAllWifiConnections();
                        break;
                    case CONNECT_TO_RC_CAR:
                        connectToRCCar();
                        break;
                    case START_WIFI_SERVER:
                        startWifiServer();
                        break;
                    case START_HLS_SERVER:
                        startHLSServer();
                        startMicStream();
                        break;
                    case STOP_HLS_SERVER:
                        stopHLSServer();
                        stopMicStream();
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
        ipServer.startServer(new IPServer.OnClientConnectedListener() {
            @Override
            public void onClientConnected(IPServer.ClientConnection clientConnection) {

                micStreamer.setDestIP(clientConnection.getClientSocket().getInetAddress().getHostAddress());

                onNewClientConnected();
            }
        }, new IPServer.OnClientDataReceivedListener() {
            @Override
            public void onClientDataReceived(IPServer.ClientConnection clientConnection, JSONObject jsonObject) {
//                Toast.makeText(MainService.this, "Data Received: " + jsonObject, Toast.LENGTH_LONG).show();

                //ipServer.sendDataToAll("Returning data - " + data);
                processWifiData(clientConnection, jsonObject);
            }
        });
    }

    public void processWifiData(IPServer.ClientConnection clientConnection, JSONObject jsonObject) {
        try {
            if (jsonObject.getBoolean(CommConstants.NAME_SUCCESS)) {
                if (jsonObject.has(CommConstants.REQUEST_NAME_ARDUINO_BLUETOOTH_DATA)) {
                    if (bluetoothMaster != null) {
                        bluetoothMaster.sendData(jsonObject.getString(CommConstants.REQUEST_NAME_ARDUINO_BLUETOOTH_DATA));
                    }
                }
                if (jsonObject.has(CommConstants.REQUEST_NAME_CONNECT_TO_RC_CAR)) {
                    connectToRCCar();
                }
                if (jsonObject.has(CommConstants.REQUEST_NAME_DISCONNECT_FROM_RC_CAR)) {
                    disconnectFromRCCar();
                }
                if (jsonObject.has(CommConstants.REQUEST_NAME_RESET_WIFI_CONNECTIONS)) {
                    resetAllWifiConnections();
                }
                if (jsonObject.has(CommConstants.REQUEST_NAME_START_WIFI_SERVER)) {
                    startWifiServer();
                }
                if (jsonObject.has(CommConstants.REQUEST_NAME_START_HLS_SERVER)) {
                    startHLSServer();
                    startMicStream();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    JSONObject responseJSONObject = new JSONObject();
                    responseJSONObject.put(CommConstants.NAME_SUCCESS, true);
                    responseJSONObject.put(CommConstants.NAME_CLIENT_REQUEST_ID, jsonObject.getInt(CommConstants.NAME_CLIENT_REQUEST_ID));
                    ipServer.sendDataToClient(clientConnection, responseJSONObject);
                }
                if (jsonObject.has(CommConstants.REQUEST_NAME_STOP_HLS_SERVER)) {
                    stopHLSServer();
                    stopMicStream();
                }
                if (jsonObject.has(CommConstants.REQUEST_NAME_SPEAK)) {
                    if(jsonObject.getBoolean(CommConstants.NAME_SUCCESS)){
                        String text = jsonObject.getString(CommConstants.NAME_SPEECH_DATA);
                        speak(text, TextToSpeech.QUEUE_ADD);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void speakStatic(String s, int queueMode){
        if(thisService!=null){
            thisService.speak(s, queueMode);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void speak(String s, int queueMode){
        if(textToSpeech!=null){
            textToSpeech.speak(s,queueMode,null,null);
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

    public void resetAllWifiConnections() {
        if (ipServer != null) {
            ipServer.resetAllConnections();
        }

        speak("All connections have been reset successfully", TextToSpeech.QUEUE_ADD);
    }

    public void connectToRCCar() {
        if (bluetoothMaster != null) {
            if (bluetoothMaster.connectToRCCar()) {
                speak("Connected to Arduino Board!", TextToSpeech.QUEUE_ADD);
            }else{
                speak("Cannot connect to Arduino board!", TextToSpeech.QUEUE_ADD);
            }
        }
    }

    public void disconnectFromRCCar(){
        if (bluetoothMaster != null) {
            bluetoothMaster.closeAllConnections();
            speak("Disconnected from Arduino Board!", TextToSpeech.QUEUE_ADD);
        }
    }

    public void startWifiServer() {
        if (ipServer != null) {
            ipServer.stopServer();
        }
        initiateWifiServer();

        speak("Restarting Wifi Server!", TextToSpeech.QUEUE_ADD);
    }

    public void startHLSServer(){
        try {
            cameraStreamer.start();
            speak("Starting live stream!", TextToSpeech.QUEUE_ADD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopHLSServer(){
        try {
            cameraStreamer.stop();
            speak("Stopping live stream!", TextToSpeech.QUEUE_ADD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startMicStream(){
        try {
            micStreamer.startStreaming();
//            speak("Mic stream started!", TextToSpeech.QUEUE_ADD);
        } catch (InvalidObjectException e) {
            e.printStackTrace();
            speak("Error starting the Mic Stream. Invalid IP!", TextToSpeech.QUEUE_ADD);
        }
    }

    public void stopMicStream(){
        try {
            micStreamer.stopStreaming();
//            speak("Mic stream stopped!", TextToSpeech.QUEUE_ADD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onNewClientConnected(){
        speak("New device connected", TextToSpeech.QUEUE_ADD);
        speak(MainService.SELF_NAME+" at your service!", TextToSpeech.QUEUE_ADD);
    }
}
