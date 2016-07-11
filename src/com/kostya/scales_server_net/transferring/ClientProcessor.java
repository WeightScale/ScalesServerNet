package com.kostya.scales_server_net.transferring;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientProcessor {
    private Socket socket;
    private final Context context;
    private final String serverIpAddress;
    private static final int TIME_OUT_CONNECT = 500; /** Время в милисекундах. */
    private static  final String TAG = "ClientProcess";

    public ClientProcessor(String textForSend, String serverIpAddress, Context context) {
        this.context = context;
        this.serverIpAddress = serverIpAddress;
        sendSimpleMessageToOtherDevice(textForSend);
    }

    public ClientProcessor(String serverIpAddress, Context context) {
        this.context = context;
        this.serverIpAddress = serverIpAddress;
    }

    public ClientProcessor(Object object, String serverIpAddress, Context context) {
        this.context = context;
        this.serverIpAddress = serverIpAddress;
        sendSimpleObjectToOtherDevice(object);
    }

    private Socket getSocket(String serverIpAddress) throws Exception {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(InetAddress.getByName(serverIpAddress), ServerSocketProcessorRunnable.SERVER_PORT), TIME_OUT_CONNECT);
        } catch (IOException e) {
            throw new Exception(e.getMessage());
        }
        return socket;
    }

    private void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {}
    }


    ///////////////////////////////////////////////////////////////////////////

    public void sendTextToOtherDevice() {
        try {
            socket = getSocket(serverIpAddress);

            PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            output.println("MESSAGE FROM CLIENT");

            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message = input.readLine();

            Log.i(TAG,"Client received : " + message);

            input.close();
            output.close();
            socket.close();

        } catch (Exception e) {
            //// TODO: 09.07.2016  
            Log.i(TAG,e.getMessage());

        } finally {
            closeSocket(socket);
        }
    }

    public void sendSimpleMessageToOtherDevice(String message) {
        try {
            socket = getSocket(serverIpAddress);

            PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            output.println(message);

            output.close();
            socket.close();

        } catch (Exception e) {
            //// TODO: 09.07.2016  
            Log.i(TAG,e.getMessage());

        } finally {
            closeSocket(socket);
        }
    }

    public void sendSimpleObjectToOtherDevice(Object object) {
        try {
            socket = getSocket(serverIpAddress);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(object);

            out.close();
            socket.close();

        } catch (Exception e) {
            //// TODO: 09.07.2016
            Log.i(TAG,e.getMessage());

        } finally {
            closeSocket(socket);
        }
    }
}
