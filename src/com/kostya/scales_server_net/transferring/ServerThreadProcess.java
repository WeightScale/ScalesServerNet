package com.kostya.scales_server_net.transferring;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Kostya  on 13.07.2016.
 */
public class ServerThreadProcess extends Thread {
    public static final int SERVER_PORT = 8700;
    private final Context context;
    private final ServerSocket serverSocket;
    private ArrayList<ServerThread> serverThreads = new ArrayList<ServerThread>();
    private static final String TAG = ServerThreadProcess.class.getName();

    interface InterfaceInterrupt{
        void onInterrupt(ServerThread thread);
    }

    public ServerThreadProcess(Context context) throws IOException {
        this.context = context;
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(SERVER_PORT));
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                ServerThread thread = new ServerThread(context, serverSocket.accept(), interfaceInterrupt);
                thread.start();
                serverThreads.add(thread);
                //new ServerThread(context, serverSocket.accept()).start();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    InterfaceInterrupt interfaceInterrupt = new InterfaceInterrupt() {
        @Override
        public void onInterrupt(ServerThread thread) {
            serverThreads.remove(thread);
        }
    };

    public void closedSocket() throws IOException {
        if (serverSocket != null)
            serverSocket.close();
    }

    public ArrayList<ServerThread> getServerThreads() {return serverThreads;}
}
