package com.azeesoft.rccardriver.tools.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

/**
 * Created by azizt on 5/19/2017.
 */

public class RCBluetoothMaster {

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothSocket bSocket=null;
    BufferedReader bReader=null;
    BufferedWriter bWriter=null;

    public RCBluetoothMaster(){

    }

    public boolean connectToRCCar(){
        closeAllConnections();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        for (BluetoothDevice bDev : bluetoothAdapter.getBondedDevices()) {
            if (bDev.getName().equals("HC-06")) {
                try {
                    bSocket = bDev.createInsecureRfcommSocketToServiceRecord(bDev.getUuids()[0].getUuid());
                    bSocket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("",e.getMessage());
                    try {
                        Log.e("","trying fallback...");

                        bSocket =(BluetoothSocket) bDev.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(bDev,1);
                        bSocket.connect();

                        Log.e("","Connected");
                    }
                    catch (Exception e2) {
                        Log.e("", "Couldn't establish Bluetooth connection!");
                    }
                }
            }
        }

        if(bSocket!=null && bSocket.isConnected()){
            try {
                OutputStream outputStream = bSocket.getOutputStream();
                InputStream inStream = bSocket.getInputStream();

                bReader = new BufferedReader(new InputStreamReader(inStream));
                bWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                sendData("AZEE_HANDSHAKE");
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void sendData(String msg){
        try {
            if(bWriter!=null && isConnected()) {
                bWriter.write(msg+"\n");
                bWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeAllConnections(){
        try {
            if(bReader!=null)
                bReader.close();
            if(bWriter!=null)
                bWriter.close();
            if(bSocket!=null)
                bSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected(){
        return (bSocket!=null && bSocket.isConnected());
    }
}
