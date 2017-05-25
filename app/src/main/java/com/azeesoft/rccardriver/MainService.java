package com.azeesoft.rccardriver;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.azeesoft.rccardriver.tools.bluetooth.RCBluetoothMaster;
import com.azeesoft.rccardriver.tools.screen.ScreenOnOffReceiver;
import com.azeesoft.rccardriver.tools.wifi.CommConstants;
import com.azeesoft.rccardriver.tools.wifi.IPServer;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.video.VideoQuality;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MainService extends Service {


    public enum SERVICE_INTENT_EXTRAS {RESET_CONNECTIONS, CONNECT_TO_RC_CAR, START_WIFI_SERVER, START_RTSP_SERVER, STOP_RTSP_SERVER, NEW_CLIENT_CONNECTED}

    public final static String SELF_NAME = "Zerone";

    public final static String SERVICE_INTENT_ACTION = "com.azeesoft.rccardriver.action.SERVICE_INTENT_ACTION";
    public final static String SERVICE_INTENT_EXTRA_NAME = "com.azeesoft.rccardriver.extra.SERVICE_INTENT_EXTRA_NAME";


    private static String LOG_TAG = "MainService";

    private ScreenOnOffReceiver screenOnOffReceiver = new ScreenOnOffReceiver();

    private IPServer ipServer;
    private RCBluetoothMaster bluetoothMaster;

    private static MainService thisService;


    private SurfaceView streamSurfaceView;
    private Session libStreamSession;
    private RtspServer rtspServer;

    private TextToSpeech textToSpeech;

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

        streamSurfaceView = (SurfaceView) LayoutInflater.from(this).inflate(R.layout.stream_surface_view, null);

        libStreamSession = SessionBuilder.getInstance()
                .setCallback(new Session.Callback() {
                    @Override
                    public void onBitrateUpdate(long bitrate) {

                    }

                    @Override
                    public void onSessionError(int reason, int streamType, Exception e) {

                    }

                    @Override
                    public void onPreviewStarted() {

                    }

                    @Override
                    public void onSessionConfigured() {
                        libStreamSession.start();
                    }

                    @Override
                    public void onSessionStarted() {

                    }

                    @Override
                    public void onSessionStopped() {
                        libStreamSession.stop();
                    }
                })
                .setSurfaceView(streamSurfaceView)
                .setCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)
                .setPreviewOrientation(180)
                .setContext(this)
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setAudioQuality(new AudioQuality(16000, 32000))
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setVideoQuality(new VideoQuality(320, 240, 15, 300000))
                .build();


        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    Log.d(LOG_TAG, "TTS Initialized!");
                    textToSpeech.setLanguage(Locale.US);
                    textToSpeech.setSpeechRate(1.0f);
                    speak(SELF_NAME + " at your service sir.", TextToSpeech.QUEUE_FLUSH);
                }
            }
        });
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
                    case START_RTSP_SERVER:
                        startRTSPStreamServer();
                        break;
                    case STOP_RTSP_SERVER:
                        stopRTSPStreamServer();
                        break;
                    case NEW_CLIENT_CONNECTED:
                        onNewClientConnected();
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
                if (jsonObject.has(CommConstants.REQUEST_NAME_START_RTSP_SERVER)) {
                    startRTSPStreamServer();
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
                if (jsonObject.has(CommConstants.REQUEST_NAME_STOP_RTSP_SERVER)) {
                    stopRTSPStreamServer();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param text The string of text to be spoken. No longer than
     *            {@link #getMaxSpeechInputLength()} characters.
     * @param queueMode The queuing strategy to use, {@link #QUEUE_ADD} or {@link #QUEUE_FLUSH}.
     *
     */
    public static void speakStatic(String s, int queueMode){
        if(thisService!=null){
            thisService.speak(s, queueMode);
        }
    }

    /**
     * @param text The string of text to be spoken. No longer than
     *            {@link #getMaxSpeechInputLength()} characters.
     * @param queueMode The queuing strategy to use, {@link #QUEUE_ADD} or {@link #QUEUE_FLUSH}.
     *
     */
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

    public void startRTSPStreamServer() {
        rtspServer = new RtspServer();
        rtspServer.addCallbackListener(new RtspServer.CallbackListener() {
            @Override
            public void onError(RtspServer server, Exception e, int error) {

            }

            @Override
            public void onMessage(RtspServer server, int message) {
                Log.d(LOG_TAG, "onMessage: "+message);
                if (message == RtspServer.MESSAGE_STREAMING_STARTED) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainService.this, "Streaming Started", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        rtspServer.start();

        speak("Starting Live Stream", TextToSpeech.QUEUE_ADD);
//        startService(new Intent(this, RtspServer.class));

//        Toast.makeText(this, "Stream Session started!",Toast.LENGTH_LONG).show();
    }

    public void stopRTSPStreamServer() {
        if (libStreamSession.isStreaming()) {
            libStreamSession.stop();
        }

        rtspServer.stop();
        speak("Stopping Live Stream", TextToSpeech.QUEUE_ADD);
    }

    public void onNewClientConnected(){
        speak("New Device Connected", TextToSpeech.QUEUE_ADD);
    }
}
