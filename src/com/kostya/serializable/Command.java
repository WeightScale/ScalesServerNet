package com.kostya.serializable;

import android.content.Context;
import android.net.wifi.WifiManager;
import com.kostya.scales_server_net.Globals;
import com.kostya.scales_server_net.provider.SystemTable;
import com.kostya.terminals.Terminals;

/**
 * @author Kostya on 08.07.2016.
 */
public class Command {
    static Context mContext;
    private InterfaceCommands interfaceCommands;

    public interface InterfaceCommands {
        String command(Commands commands);
    }
    public Command(Context context){
        mContext = context;
    }
    public Command(Context context, InterfaceCommands i){
        mContext = context;
        interfaceCommands = i;
    }



    public static Context getContext(){ return mContext; }

    /** Выполнить комманду получить данные.
     * @return Данные выполненой комманды. */
    public String getData(Commands cmd){
        return interfaceCommands.command(cmd);
    }

    public boolean setData(Commands cmd, String data){
        cmd.prepare(data);
        return interfaceCommands.command(cmd).equals(cmd.name());
    }

    /*private static Commands contains(String s){
        for(Commands choice : Commands.values())
            if (s.startsWith(choice.name)){
                return choice;
            }
        return null;
    }

    public static Commands execute(String inputLine) {
        try {
            Commands cmd = contains(inputLine);
            String sub = inputLine.replace(cmd.name, "");
            cmd.prepare("");
            if (sub.isEmpty())
                cmd.prepare(cmd.fetch(mContext));
            else
                cmd.setup(mContext,sub);
            return cmd;
        }catch (NullPointerException e){
            return null;
        }
    }*/

    public void setInterfaceCommand(InterfaceCommands i){
        interfaceCommands = i;
    }
}
