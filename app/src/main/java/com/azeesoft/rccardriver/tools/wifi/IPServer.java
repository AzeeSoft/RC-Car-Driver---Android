package com.azeesoft.rccardriver.tools.wifi;

import android.content.Intent;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.util.Log;

import com.azeesoft.rccardriver.MainService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by azizt on 5/20/2017.
 */

public class IPServer {

    final String LOG_TAG = "IP Server";

    private static long defaultCallbackTimeout = 10000;

    public final static int DEFAULT_PORT = 6060;
    private int serverPort = DEFAULT_PORT;
    private int maxClientConnecttions = 1;
    private ServerSocket serverSocket;
    private ServerRunnable serverClientAcceptionRunnable;
    private ArrayList<ClientConnection> clientConnections = new ArrayList<>();
    private Handler updateOnUIHandler;
    private OnClientDataReceivedListener onClientDataReceivedListener;

    private static IPServer thisServer;

    public static IPServer getIPServer(int port, int maxClients) {
        if (thisServer == null) {
            thisServer = new IPServer(port, maxClients);
        }

        return thisServer;
    }

    private IPServer(int port, int maxClients) {
        serverPort = port;
        maxClientConnecttions = maxClients;
        updateOnUIHandler = new Handler();
    }

    public boolean startServer(OnClientDataReceivedListener onClientDataReceivedListener) {
        setOnClientDataReceivedListener(onClientDataReceivedListener);
        return startServer();
    }

    public boolean startServer() {
        try {
            serverSocket = new ServerSocket(serverPort);
            serverClientAcceptionRunnable = new ServerRunnable();
            new Thread(serverClientAcceptionRunnable).start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void stopServer() {
        resetAllConnections();
        try {
            if (serverClientAcceptionRunnable != null)
                serverClientAcceptionRunnable.canRun = false;
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOnClientDataReceivedListener(OnClientDataReceivedListener onClientDataReceivedListener) {
        this.onClientDataReceivedListener = onClientDataReceivedListener;
    }

    public void sendDataToAll(JSONObject data) {
        Log.d(LOG_TAG, "Sending data to all..");
        for (ClientConnection clientConnection : clientConnections) {
            Log.d(LOG_TAG, "Sending data to client..");
            clientConnection.sendData(data);
        }
    }

    public void sendDataToClient(ClientConnection clientConnection, JSONObject data){
        clientConnection.sendData(data);
    }

    public void resetAllConnections() {
        Log.d(LOG_TAG, "Resetting Client Connections!");
        for (ClientConnection clientConnection : clientConnections) {
            clientConnection.closeConnection();
            clientConnections.remove(clientConnection);
            Log.d(LOG_TAG, "Removed Client Connection!");
        }
        Log.d(LOG_TAG, "Client Connections Size: "+clientConnections.size());
    }

    private class ServerRunnable implements Runnable {

        boolean canRun = true;

        @Override
        public void run() {
            Socket incomingSocket;

            while (!Thread.currentThread().isInterrupted() && canRun) {
                try {
                    incomingSocket = serverSocket.accept();
                    Log.d(LOG_TAG, "Incoming connection accepted");
                    if (!canRun)
                        break;

                    for (ClientConnection clientConnection : clientConnections) {
                        if (!clientConnection.getClientSocket().isConnected()) {
                            clientConnection.closeConnection();
                            clientConnections.remove(clientConnection);
                        }
                    }

                    if (clientConnections.size() < maxClientConnecttions) {

                        Log.d(LOG_TAG, "Creating client connection");
                        ClientConnection clientConnection = new ClientConnection(incomingSocket);

                        Log.d(LOG_TAG, "Adding client connection");
                        clientConnections.add(clientConnection);

                        MainService.speakStatic("New Connection Received", TextToSpeech.QUEUE_ADD);
                    } else {
                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(incomingSocket.getOutputStream()));

                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put(CommConstants.NAME_SUCCESS, false);

                            JSONArray flagsArray = new JSONArray();
                            flagsArray.put(CommConstants.RESPONSE_DATA_FLAGS_FAILURE.MAX_CONN_REACHED);

                            jsonObject.put(CommConstants.NAME_FLAGS_ARRAY, flagsArray);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        bufferedWriter.write(jsonObject + "\n");
                        bufferedWriter.flush();
                        bufferedWriter.close();
                        incomingSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class ClientConnection {

        private Socket clientSocket;
        private BufferedWriter bufferedWriter;
        private int requestId = 0;

        private ArrayList<OnResponseReceivedCallback> onResponseReceivedCallbacks = new ArrayList<>();

        ClientConnection(Socket cSocket) {
            clientSocket = cSocket;
            try {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                startListening();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void startListening() throws IOException {
            new Thread(new ClientListenerRunnable(this, new BufferedReader(new InputStreamReader(clientSocket.getInputStream())))).start();
        }

        void sendData(JSONObject data){
            sendData(data, null);
        }

        void sendData(JSONObject data, @Nullable OnResponseReceivedCallback onResponseReceivedCallback) {
            if (bufferedWriter != null && clientSocket != null && clientSocket.isConnected()) {
                try {
                    data.put(CommConstants.NAME_SERVER_REQUEST_ID, requestId);
                    Log.d(LOG_TAG, "Writing data..");
                    bufferedWriter.write(data + "\n");
                    bufferedWriter.flush();
                    Log.d(LOG_TAG, "Data written");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (onResponseReceivedCallback != null) {
                    onResponseReceivedCallback.setRequestId(requestId);
                    onResponseReceivedCallbacks.add(onResponseReceivedCallback);
                }
                requestId++;
            }
        }

        void closeConnection() {
            try {
                if (bufferedWriter != null)
                    bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (clientSocket != null)
                    clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public Socket getClientSocket() {
            return clientSocket;
        }

        public ArrayList<OnResponseReceivedCallback> getOnResponseReceivedCallbacks() {
            return onResponseReceivedCallbacks;
        }
    }

    private class ClientListenerRunnable implements Runnable {

        private ClientConnection clientConnection;
        private BufferedReader bufferedReader;

        ClientListenerRunnable(ClientConnection clientConnection, BufferedReader bReader) {
            this.clientConnection = clientConnection;
            bufferedReader = bReader;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {

//                    Log.d(LOG_TAG, "Running listening thread!");
                    if (bufferedReader != null && !clientConnection.getClientSocket().isClosed()) {
                        String incomingData = bufferedReader.readLine();
//                        Log.d(LOG_TAG, "Read Incoming Data: "+incomingData);
                        if(incomingData==null){
                            break;
                        }

                        if (!incomingData.isEmpty()) {
//                            Log.d(LOG_TAG, "Incoming data from client: " + incomingData);

                            JSONObject jsonObject;
                            try {
                                jsonObject = new JSONObject(incomingData);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("success", false);
                                    jsonObject.put("flag", "non_json_data");
                                    jsonObject.put("non_json_data", incomingData);
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }
                            }

                            ArrayList<OnResponseReceivedCallback> onResponseReceivedCallbacks = clientConnection.getOnResponseReceivedCallbacks();

                            OnResponseReceivedCallback currentCallback = null;
                            List<OnResponseReceivedCallback> timedOutCallbacks = new ArrayList<>();
                            for (OnResponseReceivedCallback callback : onResponseReceivedCallbacks) {
                                try {
                                    if(callback.getTimeout()<System.currentTimeMillis() - callback.getInitMillis()){
                                        timedOutCallbacks.add(callback);
                                    }
                                    else if (jsonObject.has(CommConstants.NAME_SERVER_REQUEST_ID)) {
                                        if (callback.getRequestId() == jsonObject.getInt(CommConstants.NAME_SERVER_REQUEST_ID)) {
                                            currentCallback = callback;
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            onResponseReceivedCallbacks.removeAll(timedOutCallbacks);
                            timedOutCallbacks.clear();

                            if (currentCallback != null) {
                                currentCallback.onResponseReceived(clientConnection, jsonObject);
                                onResponseReceivedCallbacks.remove(currentCallback);
                            } else {
                                updateOnUIHandler.post(new UpdateUIRunnable(clientConnection, jsonObject));
                            }
                        }
                    }else{
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            clientConnection.closeConnection();
            clientConnections.remove(clientConnection);

            MainService.speakStatic("Device connection lost!", TextToSpeech.QUEUE_ADD);
        }
    }

    private class UpdateUIRunnable implements Runnable {

        ClientConnection clientConnection;
        JSONObject jsonObject;

        UpdateUIRunnable(ClientConnection clientConnection, JSONObject jsonObject) {
            this.clientConnection = clientConnection;
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            if (onClientDataReceivedListener != null) {
                onClientDataReceivedListener.onClientDataReceived(clientConnection, jsonObject);
            }
        }
    }

    public interface OnClientDataReceivedListener {
        void onClientDataReceived(ClientConnection clientConnection, JSONObject jsonObject);
    }

    public static abstract class OnResponseReceivedCallback {
        private int requestId;
        private long initMillis = 0;
        private long timeout = defaultCallbackTimeout;

        public abstract void onResponseReceived(ClientConnection clientConnection, JSONObject jsonObject);

//        abstract void onResponseTimedOut();

        private void setRequestId(int reqId) {
            requestId = reqId;
            initMillis = System.currentTimeMillis();
        }

        public int getRequestId() {
            return requestId;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public long getTimeout() {
            return timeout;
        }

        public long getInitMillis() {
            return initMillis;
        }
    }
}
