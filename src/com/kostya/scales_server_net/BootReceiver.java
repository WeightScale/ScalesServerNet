/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.kostya.scales_server_net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.kostya.scales_server_net.service.ServiceScalesNet;

/**
 * @author Kostya
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, ServiceScalesNet.class));//
        /*
        Intent testIntent = new Intent(context, Test.class);
        testIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(testIntent);*/
    }

}
