package com.azeesoft.rccardriver.tools.wifi;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.azeesoft.rccardriver.MainService;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by azizt on 5/28/2017.
 */

public class MicStreamer {
    public byte[] buffer;
    public static DatagramSocket socket;
    private String destIP = "";
    private int port = 50005;

    AudioRecord recorder;

    private int sampleRate = 44100; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean isStreaming = false;

    private static MicStreamer thisStreamer;

    public static MicStreamer getMicStreamer(String destIP) {
        if (thisStreamer == null) {
            thisStreamer = new MicStreamer(destIP);
        }else{
            thisStreamer.setDestIP(destIP);
        }
        return thisStreamer;
    }

    private MicStreamer(String destIP) {
        setDestIP(destIP);
    }

    public void setDestIP(String ip) {
        destIP = ip;
    }

    public boolean hasValidIP(){
        return !destIP.equals("");
    }

    public void startStreaming() throws InvalidObjectException {

        if(!hasValidIP()) {
            throw new InvalidObjectException("Invalid IP!");
        }

        if (isStreaming) {
            stopStreaming();
        }

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    DatagramSocket socket = new DatagramSocket();
                    Log.d("VS", "Socket Created");

                    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                    byte[] buffer = new byte[minBufSize];

                    Log.d("VS", "Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName(destIP);
                    Log.d("VS", "Address retrieved");


                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 10);
                    Log.d("VS", "Recorder initialized");

                    recorder.startRecording();


                    while (isStreaming) {
                        //reading data from MIC into buffer
                        minBufSize = recorder.read(buffer, 0, buffer.length);

                        //putting buffer in the packet
                        packet = new DatagramPacket(buffer, buffer.length, destination, port);

                        socket.send(packet);
                        System.out.println("MinBufferSize: " + minBufSize);
                    }
                } catch (UnknownHostException e) {
                    Log.e("VS", "UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("VS", "IOException");
                } catch (Exception e){
                    e.printStackTrace();
                }

                isStreaming = false;
            }

        });
        isStreaming = true;
        streamThread.start();
    }

    public void stopStreaming() {
        if (recorder != null) {
            recorder.release();
            isStreaming = false;
        }
    }
}
