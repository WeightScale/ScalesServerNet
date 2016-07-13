package com.kostya.scales_server_net.transferring;

import android.content.Context;
import com.kostya.serializable.CommandObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by Kostya on 13.07.2016.
 */
public class ServerThread extends Thread {
    private Context context;
    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private ServerThreadProcess.InterfaceInterrupt interfaceInterrupt;

    public ServerThread(Context context, Socket accept, ServerThreadProcess.InterfaceInterrupt in) {
        this.context = context;
        socket = accept;
        interfaceInterrupt = in;
    }
    public ServerThread(Context context, Socket accept) {
        this.context = context;
        socket = accept;
    }

    @Override
    public void run() {
        try {
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            while (!isInterrupted()){
                Object object = objectInputStream.readObject();
                if (object !=null){
                    ((CommandObject)object).execute(context, objectOutputStream);

                }
            }
        } catch (Exception e) {
            closeSocket();
        }finally{
            closeSocket();
        }
        interrupt();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        interfaceInterrupt.onInterrupt(this);
    }

    private void closeSocket() {
        try {objectOutputStream.close();} catch (IOException e) {}
        try {objectInputStream.close();} catch (IOException e) {}
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {}
    }

    void writeObject(Object o) throws IOException {
        objectOutputStream.writeObject(o);
    }
}
