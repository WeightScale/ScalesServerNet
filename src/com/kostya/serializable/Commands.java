package com.kostya.serializable;

import android.content.Context;
import android.net.wifi.WifiManager;
import com.kostya.scales_server_net.Globals;
import com.kostya.scales_server_net.provider.SystemTable;
import com.kostya.terminals.Terminals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by Kostya on 10.07.2016.
 */
public enum  Commands implements Serializable {
    /** Версия */
    CMD_VERSION(){

        @Override
        void setup(Context context, String d) {
        }

        @Override
        String fetch(Context context) {
            return "ServerScales";
        }
    },
    /** Имя конкретной сети WiFi. */
    CMD_SSID_WIFI(){
        @Override
        void setup(Context context, String d){
            new SystemTable(context).updateEntry(SystemTable.Name.WIFI_SSID, d);
        }

        @Override
        String fetch(Context context) {
            try {
                return new SystemTable(context).getProperty(SystemTable.Name.WIFI_SSID);
            } catch (Exception e) {
                return e.getMessage();
            }
        }
    },
    /** Ключ конкретной сети WiFi. */
    CMD_KEY_WIFI(){
        @Override
        void setup(Context context, String d){
            new SystemTable(context).updateEntry(SystemTable.Name.WIFI_KEY, d);
        }

        @Override
        String fetch(Context context) {
            try {
                return new SystemTable(context).getProperty(SystemTable.Name.WIFI_KEY);
            } catch (Exception e) {
                return e.getMessage();
            }
        }
    },
    /** Выключить WiFi для пересоединения. */
    CMD_RECONNECT_SERVER_NET() {
        @Override
        void setup(Context context, String d) {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            wifi.setWifiEnabled(false);
        }

        @Override
        String fetch(Context context) {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            wifi.setWifiEnabled(false);
            return "";
        }
    },
    /** Данные usb. */
    CMD_OUT_USB() {
        @Override
        void setup(Context context, String d) {

        }

        @Override
        String fetch(Context context) {
            return null;
        }

    },
    CMD_ERROR() {
        @Override
        void setup(Context context, String d) {

        }

        @Override
        String fetch(Context context) {
            return null;
        }
    },
    /** Выбор терминала. */
    CMD_DEFAULT_TERMINAL() {
        @Override
        void setup(Context context, String d) {
            try {
                Globals.getInstance().terminal = Terminals.values()[Integer.valueOf(d)];
                new SystemTable(context).updateEntry(SystemTable.Name.TERMINAL, d);
            }catch (Exception e){}
        }

        @Override
        String fetch(Context context) {
            return String.valueOf(Globals.getInstance().terminal.ordinal());
        }
    },
    /** Лист список терминалов. */
    CMD_LIST_TERMINALS() {
        @Override
        void setup(Context context, String d) {

        }

        @Override
        String fetch(Context context) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Terminals terminal : Terminals.values()){
                stringBuilder.append(terminal.name()).append('-').append(terminal.ordinal()).append(' ');
            }
            return stringBuilder.toString();
        }
    },
    CMD_COM_PORT {
        @Override
        void setup(Context context, String d) {

        }

        @Override
        Object fetch(Context context) {
            return Globals.getInstance().getComPortObject();
        }
    };

    private static final long serialVersionUID = 7526471155622776148L;

    private String data;
    private String command;
    abstract void setup(Context context,String d);
    abstract Object fetch(Context context);

    public String toString() { return command; }
    public void appendData(String d){
        data = d;
    }

    public void setData(String data) {this.data = data;}
    public String getData() {return data;}

    public void prepare(String data){
        command = name()+data;
    }
}