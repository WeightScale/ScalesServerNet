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
 * @author Kostya Created by Kostya on 04.07.2016.
 */
public class BluetoothBaseManager {
    Context mContext;
    private BroadcastReceiverBluetooth broadcastReceiverBluetooth;
    private BluetoothAdapter mBluetoothAdapter;
    private BufferedReader inputBufferedReader;
    private PrintWriter outputPrintWriter;
    private final Handler handler = new Handler();
    private AcceptThread acceptThread;
    private Timer bluetoothTimeout;
    private static final int TIMEOUT_BLUETOOTH = 600000; /** Время с милисекундах для таймера выключения bluetooth. */
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static  final String NAME = "ServerScales";
    private static final String TAG = BluetoothBaseManager.class.getName();
    boolean flagTimeout = false;

    public BluetoothBaseManager(Context context) throws Exception {
        mContext = context;
        Commands.setContext(mContext);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
            throw new Exception("Bluetooth adapter missing");
        mBluetoothAdapter.enable();
        /** Флаг таймаут включения bluetooth */
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mBluetoothAdapter.isEnabled())
                    flagTimeout = true;
            }
        }, 5000);
        while (!mBluetoothAdapter.isEnabled() && !flagTimeout) ;//ждем включения bluetooth или таймаут.
        if(flagTimeout)
            throw new Exception("Timeout enabled bluetooth");
        /** Приемник событий bluetooth*/
        mBluetoothAdapter.setName(NAME);
        broadcastReceiverBluetooth = new BroadcastReceiverBluetooth();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mContext.registerReceiver(broadcastReceiverBluetooth, intentFilter);
    }

    /**
     * Запуск потока для процесса Bluetooth server.
     */
    public void start(){
        acceptThread = new AcceptThread();
        acceptThread.start();
        bluetoothTimeout = new Timer();
        bluetoothTimeout.schedule(new TimerProcessBluetooth(), TIMEOUT_BLUETOOTH);
    }

    /**
     * Таймер для для запуска задачи выключения bluetooth после заданого времени.
     */
    private class TimerProcessBluetooth extends TimerTask {
        @Override
        public void run() {
            stop();
        }
    }

    /**
     * Останавливаем процесс bluetooth server и прочии процессы.
     */
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

    /**
     * Класс процесса Bluetooth Accept.
     */
    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;
        private boolean isClosedSocket = false;

        public AcceptThread() {
            /*BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);
            } catch (IOException e) { }
            mmServerSocket = tmp;*/
        }

        public void run() {
            BluetoothSocket socket = null;
            /** Пока процесс не прерван. */
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);
                    socket = mmServerSocket.accept(10000);
                    if (socket != null) {
                        /** Процедура обработки приема и отправки данных. */
                        // ViktYusk
                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                        String str1 = br.readLine();
                        String str2 = br.readLine();
                        Commands.execute(str1);
                        Commands.execute(str2);
                        //
                        processInputInputOutputBuffers(socket);
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            /** Закрываем socket  */
            try {socket.close();} catch (Exception e) {}
            cancel();
        }


        /** Устанавливаем флаг для контроля разрыва socket.
         * @param closedSocket Флаг.
         */
        public void setClosedSocket(boolean closedSocket) {
            isClosedSocket = closedSocket;
        }

        /** Закрываем socket, и вызывает завершение thread. */
        public void cancel() {
            try {
                mmServerSocket.close();
                mBluetoothAdapter.disable();
            } catch (IOException e) { }
        }

        /** Процедура обработки данных.
         * @param socket Открытый socket
         * @throws Exception Исключение если ошибка.
         */
        private void processInputInputOutputBuffers(BluetoothSocket socket) throws Exception {

            inputBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            /** Пока socket не разорван. */
            while (!isClosedSocket){
                /** Если данные готовы для чтения. */
                if (inputBufferedReader.ready()){
                    String inputLine = inputBufferedReader.readLine();
                    if (inputLine != null){
                        /** Выполняем принятую команду. */
                        Commands commands = Commands.execute(inputLine);
                        if (commands != null){
                            Log.d(TAG, "Received message : " + commands.getName());
                            /** Ответ на команду. */
                            outputPrintWriter.println(commands.getData());
                        }
                    }
                }
                Thread.sleep(10);
            }
            /** Закрываем при разрыве socket */
            try { inputBufferedReader.close(); }catch (Exception e){}
            try {outputPrintWriter.close(); }catch (Exception e){}
            try {socket.close();}catch (Exception e){}



        }

    }


    /**
     * Приемник событий Bluetooth.
     */
    public class BroadcastReceiverBluetooth extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action){
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        acceptThread.setClosedSocket(false);
                        Log.d(TAG, action);
                        break;
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        acceptThread.setClosedSocket(true);
                        Log.d(TAG, action);
                        break;
                    default:
                }

            }
        }
    }

}
