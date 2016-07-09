package com.kostya.scales_server_net.transferring;

import android.content.Context;

import java.io.IOException;
import java.net.ServerSocket;

public class JmDnsServerThreadProcessor {

    private Thread serverProcessorThread;
    private ServerSocketProcessorRunnable serverSocketProcessor;

    public void startServerProcessorThread(Context context) {
        serverSocketProcessor = new ServerSocketProcessorRunnable(context);
        serverProcessorThread = new Thread(serverSocketProcessor);
        serverProcessorThread.start();
    }

    public void stopServerProcessorThread() {

        try {
            // make sure you close the socket upon exiting
            /*if (serverSocket != null)
                serverSocket.close();*/
            serverSocketProcessor.closedSocket();

            if (serverProcessorThread != null)
                serverProcessorThread.interrupt();

        } catch (IOException e) {
            //// TODO: 09.07.2016  
        }
    }

}
