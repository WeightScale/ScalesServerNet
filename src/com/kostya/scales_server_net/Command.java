package com.kostya.scales_server_net;

import android.content.Context;
import android.net.wifi.WifiManager;
import com.kostya.scales_server_net.provider.SystemTable;

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

    public enum  Commands {
        /** Версия */
        VRS("VRS"){
            @Override
            void setup(String d) {
            }

            @Override
            String fetch() {
                return "ServerScales";
            }
        },
        /** Имя конкретной сети WiFi. */
        SSID_NET("SSID"){
            @Override
            void setup(String d){
                new SystemTable(getContext()).updateEntry(SystemTable.Name.WIFI_SSID, d);
            }

            @Override
            String fetch() {
                try {
                    return new SystemTable(getContext()).getProperty(SystemTable.Name.WIFI_SSID);
                } catch (Exception e) {
                    return e.getMessage();
                }
            }
        },
        /** Ключ конкретной сети WiFi. */
        PASS_NET("PASS"){
            @Override
            void setup(String data){
                new SystemTable(getContext()).updateEntry(SystemTable.Name.WIFI_KEY, data);
            }

            @Override
            String fetch() {
                try {
                    return new SystemTable(getContext()).getProperty(SystemTable.Name.WIFI_KEY);
                } catch (Exception e) {
                    return e.getMessage();
                }
            }
        },
        /** Выключить WiFi для пересоединения. */
        RECONNECT_NET("RCSN") {
            @Override
            void setup(String d) {
                reconnect();
            }

            @Override
            String fetch() {
                reconnect();
                return "";
            }

            void reconnect(){
                WifiManager wifi = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
                wifi.setWifiEnabled(false);
            }
        },
        /** Данные usb. */
        OUT_USB_PORT("OUSB") {
            @Override
            void setup(String d) {

            }

            @Override
            String fetch() {
                return null;
            }
        };

        private final String name;
        private static String command;

        private static String data;
        abstract void setup(String d);
        abstract String fetch();



        Commands(String name) {this.name = name;}

        public String toString() { return command; }

        public String getData() { return data; }

        public String getName() {return name;}

        public String getResponse(String substring) {
            if (substring.startsWith(getName())){
                substring = substring.replace(getName(),"");
                return substring.isEmpty() ? getName() : substring;
            }else
                return "";
        }

        public void prepare(String data){
            command = getName()+data;
        }

    }

    public static Context getContext(){ return mContext; }

    /** Выполнить комманду получить данные.
     * @return Данные выполненой комманды. */
    public String getData(Commands cmd){
        return interfaceCommands.command(cmd);
    }

    public boolean setData(Commands cmd, String data){
        cmd.prepare(data);
        return interfaceCommands.command(cmd).equals(cmd.getName());
    }

    private static Commands contains(String s){
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
                cmd.prepare(cmd.fetch());
            else
                cmd.setup(sub);
            return cmd;
        }catch (NullPointerException e){
            return null;
        }
    }

    public void setInterfaceCommand(InterfaceCommands i){
        interfaceCommands = i;
    }
}
