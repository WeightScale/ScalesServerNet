package com.kostya.serializable;



import android.content.Context;

import java.io.Serializable;

/**
 * @author Kostya  on 10.07.2016.
 */
public class CommandObject implements Serializable {
    private static final long serialVersionUID = 7526471155622776147L;
    Commands commandName;
    String value = "";
    Object object;
    public CommandObject(Commands name, String value){
        this.commandName = name;
        this.value = value;
    }

    public CommandObject(Commands name, Object object){
        this.commandName = name;
        this.object = object;
    }

    public CommandObject(Commands name){
        this.commandName = name;
    }

    public CommandObject execute(Context context){
        if (value.isEmpty())
            return new CommandObject(commandName, commandName.fetch(context));
        else{
            commandName.setup(context, value);
            return new CommandObject(commandName);
        }

    }

    public CommandObject appendValue(String v){
        value = v;
        return this;
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