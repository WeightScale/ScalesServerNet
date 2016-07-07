package com.kostya.scales_server_net;


import android.content.Context;
import com.kostya.scales_server_net.provider.SystemTable;

/**
 * @author Kostya Created by Kostya on 06.07.2016.
 */
public enum  Commands {
    /** Версия */
    CMD_VRS("VRS"){
        @Override
        void setup(String d) {
        }

        @Override
        String fetch() {
            return "ServerScales";
        }
    },
    /** Имя конкретной сети WiFi. */
    CMD_SSID_NET("SSID"){
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
    CMD_PASS_NET("PASS"){
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
    };

    private static Context context;
    private final String name;
    private static String command;

    private static String data;
    abstract void setup(String d);
    abstract String fetch();
    Commands(String name) {this.name = name;}

    public String toString() { return command; }

    private static Commands contains(String s){
        for(Commands choice : values())
            if (s.startsWith(choice.name)){
                return choice;
            }
        return null;
    }

    public static Commands execute(String inputLine) {
        try {
            Commands cmd = contains(inputLine);
            String sub = inputLine.replace(cmd.name, "");
            command = cmd.getName();
            if (sub.isEmpty())
                command += cmd.fetch();
            else
                cmd.setup(sub);
            return cmd;
        }catch (NullPointerException e){
            return null;
        }
    }

    public static void setContext(Context cnx){ context = cnx; }

    public Context getContext(){ return context; }

    public String getData() { return data; }

    public String getName() {return name;}

}
