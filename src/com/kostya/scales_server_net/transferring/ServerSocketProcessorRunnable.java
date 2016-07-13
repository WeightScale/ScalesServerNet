package com.kostya.scales_server_net.transferring;

import android.content.Context;
import android.util.Log;
import com.kostya.serializable.CommandObject;
import com.kostya.terminals.TerminalObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketProcessorRunnable implements Runnable {

    public static final int SERVER_PORT = 8700;
    private ServerSocket serverSocket;
    private final Context context;
    private BufferedReader inputBufferedReader;
    private PrintWriter outputPrintWriter;
    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;


    private static final String TAG = "SERVER_SOCKET";

    public ServerSocketProcessorRunnable( Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(SERVER_PORT));
            //serverSocket = new ServerSocket(SERVER_PORT);

        } catch (IOException e) {
            //// TODO: 09.07.2016  
        }


        try {
            while (!Thread.currentThread().isInterrupted()) {

                Log.v(TAG, "before socket ACCEPT");
                Socket socket = serverSocket.accept();
                Log.v(TAG, "ACCEPTED");

                //InputStream inputStream = socket.getInputStream();
                //InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                //inputBufferedReader = new BufferedReader(inputStreamReader);

//                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
//                BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
//                PrintWriter printWriter = new PrintWriter(bufferedWriter, true);
//                printWriter.println("some info");

                //outputPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                //processInputInputOutputBuffers();
                processInputInputOutputObject(socket);

            }

            inputBufferedReader.close();
            //outputPrintWriter.close();

            Log.v(TAG, "BUFFERS CLOSED");

        } catch (Exception ex) {
            Log.v(TAG, "server socket processor thread EXCEPTION : " + ex);

        } catch (Error error){
            Log.v(TAG, "server socket processor thread ERROR : " + error);
        }

    }


    private void processInputInputOutputBuffers() throws IOException {

        Log.v(TAG, "...SOCKET DATA PROCESSING...");

        String inputLine = inputBufferedReader.readLine();
        if (inputLine != null){
            Log.d(TAG, "Received message : " + inputLine);
            //outputPrintWriter.println("YOU TEXT ARRIVED. THANKS");
        }
    }

    private void processInputInputOutputObject(Socket socket) {

        try {
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            //objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            Object object = objectInputStream.readObject();
            if (object !=null){
                if(object instanceof CommandObject){
                    ((CommandObject)object).outputSocket(context, socket);
                    //((CommandObject)object).execute(context);
                }
            }
            /*executorService.execute(new Runnable() {
                @Override
                public void run() {
                    if (object !=null){
                        if(object instanceof CommandObject){
                            String address = socket.getRemoteSocketAddress().toString();
                            ((CommandObject)object).execute(context);
                        }else if (object instanceof TerminalObject){
                            Globals.getInstance().setCurrentTerminal((TerminalObject)object);
                            TerminalObject t = Globals.getInstance().getCurrentTerminal();
                            int s = t.getComPortObject().getSpeed();
                        }
                    }
                }
            });*/
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }finally{
            try {objectInputStream.close();} catch (IOException e1) {}
        }

    }

    public void closedSocket() throws IOException {
        if (serverSocket != null)
            serverSocket.close();
        if (inputBufferedReader != null)
            inputBufferedReader.close();
    }

}
