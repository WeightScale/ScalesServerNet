/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.kostya.scales_server_net;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.kostya.scales_server_net.service.ServiceScalesNet;
import com.kostya.scales_server_net.transferring.DataTransferringManager;

/**
 * @author Kostya
 */
public class Main extends Application {
    private static Main instance = new Main();
    private DataTransferringManager dataTransferring;
    private DataTransferringManager settingsTransferring;
    private Globals globals;
    private String deviceId = "";
    public static Context currentContext;

    @Override
    public void onCreate() {
        super.onCreate();

        globals = Globals.getInstance();
        globals.initialize(this);

        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getDeviceId() != null) {
            deviceId = tm.getDeviceId();
        } else {
            deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        dataTransferring = new DataTransferringManager(DataTransferringManager.SERVICE_INFO_TYPE_SCALES);
        settingsTransferring = new DataTransferringManager(DataTransferringManager.SERVICE_INFO_TYPE_SETTINGS);
        startService(new Intent(this,ServiceScalesNet.class));
    }

    public DataTransferringManager getDataTransferring() {
        if (dataTransferring == null)
            dataTransferring = new DataTransferringManager(DataTransferringManager.SERVICE_INFO_TYPE_SCALES);
        return dataTransferring;
    }

    public DataTransferringManager getSettingsTransferring() {
        if (settingsTransferring == null)
            settingsTransferring = new DataTransferringManager(DataTransferringManager.SERVICE_INFO_TYPE_SETTINGS);
        return settingsTransferring;
    }

    private void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public static Main getInstance() { return instance; }

    public static void setInstance(Main instance) { Main.instance = instance; }

}
