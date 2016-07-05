package com.kostya.scales_server_net;

import android.app.ProgressDialog;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Kostya on 04.07.2016.
 */
public class BluetoothBaseManager {
    Context mContext;
    private BroadcastReceiverBluetooth broadcastReceiverBluetooth;
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket socket = null;
    private BufferedReader inputBufferedReader;
    private PrintWriter outputPrintWriter;
    private final Handler handler = new Handler();
    private AcceptThread acceptThread;
    private Timer bluetoothTimeout;
    private static final int TIMEOUT_BLUETOOTH = 300000;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static  final String NAME = "ServerScales";
    private static final String TAG = BluetoothBaseManager.class.getName();
    boolean flagTimeout = false;

    public BluetoothBaseManager(Context context) throws Exception {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
            throw new Exception("Bluetooth adapter missing");
        mBluetoothAdapter.enable();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mBluetoothAdapter.isEnabled())
                    flagTimeout = true;
            }
        }, 5000);
        while (!mBluetoothAdapter.isEnabled() && !flagTimeout) ;//ждем включения bluetooth
        if(flagTimeout)
            throw new Exception("Timeout enabled bluetooth");

        broadcastReceiverBluetooth = new BroadcastReceiverBluetooth();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mContext.registerReceiver(broadcastReceiverBluetooth, intentFilter);
    }

    public void start(){
        acceptThread = new AcceptThread();
        acceptThread.start();
        bluetoothTimeout = new Timer();
        bluetoothTimeout.schedule(new TimerProcessBluetooth(), TIMEOUT_BLUETOOTH);
    }

    private class TimerProcessBluetooth extends TimerTask {
        @Override
        public void run() {
            stop();
        }
    }

    public void stop(){
        if (acceptThread != null)
            acceptThread.cancel();

        try {inputBufferedReader.close();} catch (Exception e) { }

        try {outputPrintWriter.close();} catch (Exception e) { }

        if (acceptThread != null)
            acceptThread.interrupt();
        bluetoothTimeout.cancel();
        bluetoothTimeout.purge();
        try { mContext.unregisterReceiver(broadcastReceiverBluetooth); }catch (Exception e){}

    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private boolean isClosedSocket = false;

        public AcceptThread() {

            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            // Keep listening until exception occurs or a socket is returned
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = mmServerSocket.accept();
                    if (socket != null) {
                        /** Процедура обработки приема и отправки данных. */
                        processInputInputOutputBuffers(/*socket*/);
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }
                // If a connection was accepted

            }
            try {socket.close();} catch (Exception e) {}
            cancel();
        }

        /** Закрываем socket, и вызывает завершение thread. */
        public void cancel() {
            try {
                mmServerSocket.close();
                mBluetoothAdapter.disable();
            } catch (IOException e) { }
        }


    }

    private void processInputInputOutputBuffers(/*BluetoothSocket socket*/) throws Exception {

        InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
        inputBufferedReader = new BufferedReader(inputStreamReader);
        while (socket.isConnected()){
            if (inputBufferedReader.ready()){
                String inputLine = inputBufferedReader.readLine();
                if (inputLine != null){
                    Log.d(TAG, "Received message : " + inputLine);
                    outputPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    outputPrintWriter.println("ServerScales");
                }
            }
        }
        inputBufferedReader.close();
        outputPrintWriter.close();
        socket.close();
    }

    public class BroadcastReceiverBluetooth extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action){
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        Log.d(TAG, action);
                        break;
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        try {
                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, action);
                        break;
                    default:
                }

            }
        }
    }

}
