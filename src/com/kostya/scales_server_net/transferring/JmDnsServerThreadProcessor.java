package com.kostya.scales_server_net.transferring;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class JmDnsServerThreadProcessor {
    private static final String TAG = JmDnsServerThreadProcessor.class.getName();
    //private Thread serverProcessorThread;
    //private ServerSocketProcessorRunnable serverSocketProcessor;
    private ServerThreadProcess serverThreadProcess;

    public void startServerProcessorThread(Context context) {
        /*serverSocketProcessor = new ServerSocketProcessorRunnable(context);
        serverProcessorThread = new Thread(serverSocketProcessor);
        serverProcessorThread.start();*/
        try {
            serverThreadProcess = new ServerThreadProcess(context);
            serverThreadProcess.start();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public ArrayList<ServerThread> getListThread(){
        return serverThreadProcess.getServerThreads();
    }

    public void stopServerProcessorThread() {

        try {
            // make sure you close the socket upon exiting
            /*if (serverSocket != null)
                serverSocket.close();*/
            /*serverSocketProcessor.closedSocket();

            if (serverProcessorThread != null)
                serverProcessorThread.interrupt();*/
            serverThreadProcess.closedSocket();

            if (serverThreadProcess != null)
                serverThreadProcess.interrupt();

        } catch (IOException e) {
            //// TODO: 09.07.2016  
        }
    }

}
