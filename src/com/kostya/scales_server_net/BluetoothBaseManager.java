package com.kostya.scales_server_net;

import android.app.ProgressDialog;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.*;
import java.lang.reflect.Method;
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
        broadcastReceiverBluetooth = new BroadcastReceiverBluetooth();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
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
            Commands.setContext(mContext);
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
                            outputPrintWriter.println(commands.toString());
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
                    case BluetoothDevice.ACTION_PAIRING_REQUEST:
                        try {
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                            try {
                                byte[] pin = (byte[]) BluetoothDevice.class.getMethod("convertPinToBytes", String.class).invoke(BluetoothDevice.class, "1234");
                                Method m = device.getClass().getMethod("setPin", byte[].class);
                                m.invoke(device, pin);
                                device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
                            }
                            catch(Exception e){}

                            /*device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
                            device.getClass().getMethod("cancelPairingUserInput", boolean.class).invoke(device);*/

                            /*int pin=intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, 1234);
                            int vpin = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, 0);
                            //the pin in case you need to accept for an specific pin
                            Log.d("PIN", " " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY",0));
                            //maybe you look for a name or address
                            Log.d("Bonded", device.getName());
                            byte[] pinBytes;
                            pinBytes = (""+pin).getBytes("UTF-8");
                            device.setPin(pinBytes);
                            //setPairing confirmation if neeeded
                            device.setPairingConfirmation(false);*/
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            dev.setPairingConfirmation(true);
                            //successfull pairing
                        } else {
                            Log.d(TAG, action);
                            //impossible to automatically perform pairing,
                           */ //your Android version is below KITKAT
                        break;
                    default:
                }

            }
        }
    }

}
