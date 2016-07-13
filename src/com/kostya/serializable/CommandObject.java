package com.kostya.serializable;



import android.content.Context;
import com.kostya.scales_server_net.transferring.ClientProcessor;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Kostya  on 10.07.2016.
 */
public class CommandObject implements Serializable {
    private static final long serialVersionUID = 7526471155622776147L;
    //CommandObject instance;
    //private ExecutorService executorService;
    Commands commandName;
    Object object = null;
    public CommandObject(Commands name, Object o){
        this(name);
        this.object = o;
    }

    public CommandObject(Commands name){
        //instance = this;
        commandName = name;
        //executorService = Executors.newCachedThreadPool();
    }

    public CommandObject execute(Context context){
        if (object == null){
            return new CommandObject(commandName, commandName.fetch(context));
        }else{
            commandName.setup(context, object);
            return new CommandObject(commandName);
        }
    }

    public CommandObject execute(Context context, ObjectOutputStream outputStream){
        if (object == null){
            output(context, outputStream);
        }else{
            commandName.setup(context, object);
        }
        return this;
    }

    public void outputSocket(Context context, Socket socket){
        object = commandName.fetch(context);
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
        } catch (IOException e) {}
    }

    public void output(Context context, ObjectOutputStream objectOutputStream){
        object = commandName.fetch(context);
        try {
            objectOutputStream.writeObject(this);
        } catch (IOException e) {}
    }

    public CommandObject appendObject(Object o){
        object = o;
        return this;
    }

    public void sendDevicesInNetwork(final Context context, String ipAddress){
        //CommandObject obj = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                new ClientProcessor(CommandObject.this, ipAddress, context);
            }
        }).start();
    }

    /**
     * Always treat de-serialization as a full-blown constructor, by
     * validating the final state of the de-serialized object.
     */
    /*private void readObject( ObjectInputStream aInputStream ) throws ClassNotFoundException, IOException {
        //always perform the default de-serialization first
        aInputStream.defaultReadObject();

        //make defensive copy of the mutable Date field
        //fDateOpened = new Date(fDateOpened.getTime());

        //ensure that object state has not been corrupted or tampered with maliciously
        //validateState();
    }*/

    /**
     * This is the default implementation of writeObject.
     * Customise if necessary.
     */
    /*private void writeObject( ObjectOutputStream aOutputStream ) throws IOException {
        //perform the default serialization for all non-transient, non-static fields
        aOutputStream.defaultWriteObject();
    }*/
}