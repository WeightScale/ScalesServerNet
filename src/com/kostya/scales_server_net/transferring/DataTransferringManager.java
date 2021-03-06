package com.kostya.scales_server_net.transferring;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;
import com.kostya.scales_server_net.Globals;
import com.kostya.scales_server_net.Main;
import com.kostya.scales_server_net.service.ServiceScalesNet;
import com.kostya.serializable.CommandObject;
import com.kostya.serializable.Commands;
import com.kostya.terminals.TerminalObject;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;


public class DataTransferringManager {

    public static final int SERVICE_INFO_PORT = 8856;//8856
    public static final String SERVICE_INFO_TYPE_SCALES = "_scales._tcp.local.";
    public static final String SERVICE_INFO_NAME_CLIENT = "ScalesClient";
    public static final String SERVICE_INFO_NAME_SERVER = "ScalesServer";
    private static final String SERVICE_INFO_PROPERTY_IP_VERSION = "ipv4";
    private static final String SERVICE_INFO_PROPERTY_DEVICE = "device";
    private static final String TAG = DataTransferringManager.class.getName();

    private ServiceScalesNet.OnRegisterServiceListener onRegisterServiceListener;
    private ExecutorService executorService;
    private JmDNS jmdns;
    Context context;
    private List<ServiceInfo> listClients = new ArrayList<>();
    private ServiceListener listener;
    private ServiceInfo serviceInfo;
    private MulticastLock multiCastLock;
    private final JmDnsServerThreadProcessor serverThreadProcessor = new JmDnsServerThreadProcessor();
    private final String serviceType;
    private boolean registered;


    public DataTransferringManager(Context context, String type){
        this.context = context;
        executorService = Executors.newCachedThreadPool();
        serviceType = type;
    }

    public Context getContext() {return context;}

    public DataTransferringManager(String type, ServiceScalesNet.OnRegisterServiceListener listener){
        serviceType = type;
        onRegisterServiceListener = listener;
        executorService = Executors.newCachedThreadPool();
    }

    public void setOnRegisterServiceListener(ServiceScalesNet.OnRegisterServiceListener onRegisterServiceListener) {
        this.onRegisterServiceListener = onRegisterServiceListener;
    }

    public void startDataTransferring(final Context context) {

        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        changeMultiCastLock(wifi);

        try {
            if (jmdns == null) {

                InetAddress inetAddress = getInetAddress(wifi);
                jmdns = JmDNS.create(inetAddress);
                jmdns.addServiceListener(serviceType, listener = new ServiceListener() {
                    @Override
                    public void serviceResolved(ServiceEvent ev) {
                        /** Если сервер добавляем список серверов. */
                        if (ev.getName().startsWith(SERVICE_INFO_NAME_CLIENT)){
                            listClients.add(ev.getInfo());
                            onRegisterServiceListener.onEvent(listClients.size());
                            String ip = getIPv4FromServiceInfo(serviceInfo);
                            Globals.getInstance().getLocalTerminal().setIpAddress(ip);
                            /** Посылаем локальный терминал клиенту. */
                            new CommandObject(Commands.CMD_DEFAULT_TERMINAL, Globals.getInstance().getLocalTerminal()).sendDevicesInNetwork(getContext(),getIPv4FromServiceInfo(ev.getInfo()));
                            //sendObjectToDevicesInNetwork(getContext(), getIPv4FromServiceInfo(ev.getInfo()), Globals.getInstance().getLocalTerminal());
                            onRegisterServiceListener.onEvent(jmdns.list(serviceType).length);
                        }
                    }

                    @Override
                    public void serviceRemoved(ServiceEvent ev) {
                        //ServiceInfo[] info = jmdns.list(SERVICE_INFO_TYPE);
                        onRegisterServiceListener.onEvent(jmdns.list(serviceType).length);
                        Log.i(TAG, "Service removed " + ev.getName());
                    }

                    @Override
                    public void serviceAdded(ServiceEvent event) {
                        jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
                    }
                });
                Hashtable<String, String> settings = setSettingsHashTable(context);
                serviceInfo = ServiceInfo.create(serviceType, SERVICE_INFO_NAME_SERVER , SERVICE_INFO_PORT, 0, 0, true, settings);
                jmdns.registerService(serviceInfo);
                serverThreadProcessor.startServerProcessorThread(context);
            }
        } catch (IOException e) {
            //// TODO: 09.07.2016  
        }
    }

    public void registerService(){
        if (jmdns != null)
            try {
                jmdns.registerService(serviceInfo);
            } catch (IOException e) {
                //// TODO: 09.07.2016  
            }
    }

    public void unregisterService(){
        if (jmdns != null)
            jmdns.unregisterService(serviceInfo);
    }

    public void stopDataTransferring() {
        if (jmdns != null) {
            if (listener != null) {
                jmdns.removeServiceListener(serviceType, listener);
                listener = null;
            }
            jmdns.unregisterAllServices();
            registered = false;
            try {
                jmdns.close();
            } catch (Exception ex) {
                //// TODO: 09.07.2016  
            }

            jmdns = null;
        }
        serverThreadProcessor.stopServerProcessorThread();
        if (multiCastLock != null && multiCastLock.isHeld())
            multiCastLock.release();
        executorService.shutdown();
    }

    private InetAddress getInetAddress(WifiManager wifiManager) throws IOException {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int addressIntIp = wifiInfo.getIpAddress();

        byte[] byteAddress = {
                (byte) (addressIntIp & 0xff),
                (byte) (addressIntIp >> 8 & 0xff),
                (byte) (addressIntIp >> 16 & 0xff),
                (byte) (addressIntIp >> 24 & 0xff)
        };
        return InetAddress.getByAddress(byteAddress);
    }

    private void changeMultiCastLock(WifiManager wifiManager) {
        if (multiCastLock != null && multiCastLock.isHeld())
            multiCastLock.release();

        if (multiCastLock == null) {
            multiCastLock = wifiManager.createMulticastLock("mylockthereturn");
            multiCastLock.setReferenceCounted(true);
        }

        multiCastLock.acquire();
    }

    private Hashtable<String, String> setSettingsHashTable(Context context) {
        Hashtable<String, String> settings = new Hashtable<>();
        settings.put(SERVICE_INFO_PROPERTY_DEVICE, ((Main) context.getApplicationContext()).getDeviceId());
        settings.put(SERVICE_INFO_PROPERTY_IP_VERSION, IPUtils.getLocalIpAddress(context));
        return settings;
    }

    public JmDNS getJmDNS() {
        return jmdns;
    }

    private String getIPv4FromServiceInfo(ServiceInfo serviceInfo) {
        return serviceInfo.getPropertyString(SERVICE_INFO_PROPERTY_IP_VERSION);
    }

    public void sendObjectToAllDevicesInNetwork(final Context context, Object object){
        if (jmdns != null) {

            if (executorService.isShutdown())
                executorService = Executors.newCachedThreadPool();

            Set<String> ipAddressesSet = getNeighborDevicesIpAddressesSet(context);
            for (String ipAddress : ipAddressesSet) {
                ((CommandObject)object).sendDevicesInNetwork(context, ipAddress);
                //sendObjectToDevicesInNetwork(context, ipAddress, object);
            }
        }
    }

    public void sendObjectToDevicesInNetwork(final Context context, String ipAddress, Object object){
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                new ClientProcessor(object, ipAddress, context);
            }
        });
    }

    public void sendMessageToAllDevicesInNetwork(final Context context, String message){
        if (jmdns != null) {

            if (executorService.isShutdown())
                executorService = Executors.newCachedThreadPool();

            Set<String> ipAddressesSet = getNeighborDevicesIpAddressesSet(context);
            for (String serverIpAddress : ipAddressesSet) {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        new ClientProcessor(message, serverIpAddress, context);
                    }
                });
            }
        }
    }

    private Set<String> getNeighborDevicesIpAddressesSet(Context context){

        Set<String> ipAddressesSet = new HashSet<>();
        ServiceInfo[] serviceInfoList = jmdns.list(serviceType);

        for (ServiceInfo currentServiceInfo : serviceInfoList) {
            String device = currentServiceInfo.getPropertyString(SERVICE_INFO_PROPERTY_DEVICE);
            String ownDeviceId = ((Main) context.getApplicationContext()).getDeviceId();

            if (!ownDeviceId.equals(device)) {
                String serverIpAddress = getIPv4FromServiceInfo(currentServiceInfo);
                ipAddressesSet.add(serverIpAddress);
            }
        }
        return ipAddressesSet;
    }

    public List<String> getOnlineDevicesList(Context context, String deviceId) {

        List<String> onlineDevices = new ArrayList<>();
        try {
            if (jmdns == null) {
                startDataTransferring(context);
            }

            ServiceInfo[] serviceInfoList = jmdns.list(serviceType);
            if (serviceInfoList != null) {

                for (ServiceInfo aServiceInfoList : serviceInfoList) {
                    String device = aServiceInfoList.getPropertyString(SERVICE_INFO_PROPERTY_DEVICE);

                    try {
                        if (!device.equals(deviceId)) {
                            String ip = getIPv4FromServiceInfo(aServiceInfoList);
                            if (!onlineDevices.contains(ip))
                                onlineDevices.add(ip);
                        }
                    } catch (NullPointerException e) {
                        //// TODO: 09.07.2016  
                    }
                }
            }
        } catch (Exception ex) {
            //// TODO: 09.07.2016  
        }

        return onlineDevices;
    }

}
