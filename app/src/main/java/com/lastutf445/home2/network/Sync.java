package com.lastutf445.home2.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.widget.EditText;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;

public class Sync {

    public static final int DEFAULT_PORT = 44501;

    public static final int SYNC_DASHBOARD = 0;
    public static final int SYNC_MESSAGES = 1;
    public static final int SYNC_NOTIFICATIONS = 2;
    public static final int SYNC_PING = 3;

    public static final int PROVIDER_DASHBOARD = 0;
    public static final int PROVIDER_MESSAGES = -1;
    public static final int PROVIDER_NOTIFICATIONS = -2;
    public static final int PROVIDER_DISCOVERER = -3;
    public static final int PROVIDER_NODE_IMPORT = -4;
    public static final int PROVIDER_MODULE_EDIT_REQUEST = -5;
    public static final int PROVIDER_GET_PUBLIC_KEY = -6;
    public static final int PROVIDER_CREDENTIALS = -7;
    public static final int PROVIDER_EDITOR = -8;
    public static final int PROVIDER_PING = -9;

    public static final int FRAGMENT_DASHBOARD_TRIGGER = 0;
    public static final int MENU_SYNC_TRIGGER = 1;

    public static final int UNEXPECTED_ERROR = 0;
    public static final int TOO_MANY_CLIENTS = 1;
    public static final int TOO_MANY_TASKS = 2;
    public static final int MALFORMED_PACKET = 3;
    public static final int MALFORMED_RSA = 4;
    public static final int MALFORMED_AES = 5;
    public static final int UNAUTHORIZED = 6;
    public static final int ENCRYPT_ERROR = 7;
    public static final int ENCODE_ERROR = 8;
    public static final int UNSUPPORTED = 9;
    public static final int UNKNOWN_USER = 10;
    public static final int PONG = 11;
    public static final int OK = 12;

    /*
    public static final int = ;
    public static final int = ;
    public static final int = ;
    public static final int = ;
    public static final int = ;
    public static final int = ;
    */

    private static ConnectivityManager connectivityManager = (ConnectivityManager) DataLoader.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    private static WifiManager wifiManager = (WifiManager) DataLoader.getAppContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

    private static SparseArray<Runnable> triggers = new SparseArray<>();
    private static InetAddress broadcastAddress, local;
    private static String networkBSSID = null;
    private static int networkState = 0;

    private static final SparseArray<SyncProvider> syncing = new SparseArray<>();
    private static final HashSet<Integer> removed = new HashSet<>();

    public static void init() {
        Log.d("LOGTAG", "sync initialization...");

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        connectivityManager.registerNetworkCallback(
                builder.build(),
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                        updateNetworkState();
                    }

                    @Override
                    public void onAvailable(Network network) {
                        updateNetworkState();
                    }

                    @Override
                    public void onUnavailable() {
                        updateNetworkState();
                    }

                    @Override
                    public void onLost(Network network) {
                        updateNetworkState();
                    }
                }
        );

        start();
    }

    public static void updateNetworkState() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                setNetworkState(1, null);
                return;
            }

            setNetworkState(2, wifiInfo.getBSSID());

            try {
                broadcastAddress = getBroadcastAddress();
                local = getLocalAddress();

            } catch (UnknownHostException e) {
                broadcastAddress = InetAddress.getLoopbackAddress();
                local = InetAddress.getLoopbackAddress();
                e.printStackTrace();
            }
        }

        else setNetworkState(0, null);

        for (int i = 0; i < triggers.size(); ++i) {
            (new Thread(triggers.valueAt(i))).start();
        }

        Log.d("LOGTAG", "onupdatenetworkstate triggers.size() = " + triggers.size());
    }

    private static InetAddress getLocalAddress() throws UnknownHostException {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return getInetAddress(wifiInfo.getIpAddress());
    }

    private static InetAddress getBroadcastAddress() throws UnknownHostException {
        DhcpInfo dhcp = wifiManager.getDhcpInfo();

        if(dhcp == null) {
            return InetAddress.getByName("255.255.255.255");
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        return getInetAddress(broadcast);
    }

    private static InetAddress getInetAddress(int address) throws UnknownHostException {
        byte[] quads = new byte[4];

        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((address >> k * 8) & 0xFF);
        }

        return InetAddress.getByAddress(quads);
    }

    /**
     * RETURN CODES:
     * 0 - unexpected error
     * 1 - valid
     * 2 - invalid ip
     * 3 - invalid port
     */

    public static int validateAddress(String raw_ip, String raw_port) {
        try {
            if (raw_ip.length() == 0) {
                throw new UnknownHostException("Null-length address");
            }

            InetAddress.getByName(raw_ip);
            Integer.valueOf(raw_port);
            return 1;

        } catch (UnknownHostException e) {
            //e.printStackTrace();
            return 2;

        } catch (NetworkOnMainThreadException e) {
            //e.printStackTrace();
            return 2;

        } catch (NumberFormatException e) {
            //e.printStackTrace();
            return 3;

        } catch (Exception e) {
            //e.printStackTrace();
            return 0;
        }
    }

    private synchronized static void setNetworkState(int mode, String bssid) {
        Log.d("LOGTAG", mode + " - networkstate, " + (bssid != null ? bssid : "non-bssid"));
        networkState = mode;
        networkBSSID = bssid;
    }

    public synchronized static InetAddress getBroadcast() {
        return broadcastAddress;
    }

    public synchronized static InetAddress getLocal() {
        return local;
    }

    public synchronized static String getNetworkBSSID() {
        return networkBSSID;
    }

    public static int getNetworkState() {
        return networkState;
    }

    public static SparseArray<SyncProvider> getSyncing() {
        synchronized (syncing) {
            return syncing;
        }
    }

    public static HashSet<Integer> getRemoved() {
        synchronized (removed) {
            return removed;
        }
    }

    public static void addSyncProvider(@NonNull SyncProvider provider) {
        synchronized (syncing) {
            synchronized (removed) {
                removed.remove(provider.getSource());
                syncing.put(provider.getSource(), provider);
            }
        }
    }

    public static void callProvider(int source, JSONObject data) {
        synchronized (syncing) {
            SyncProvider provider = syncing.get(source);
            if (provider == null) return;
            provider.onReceive(data);
        }
    }

    public static void removeSyncProvider(int source) {
        synchronized (removed) {
            removed.add(source);
        }
    }

    public static void start() {
        Sender.start();
        Receiver.start();
    }

    public static void stop() {
        Sender.stop();
        Receiver.stop();
    }

    public static void restart() {
        Receiver.stop();
        Sender.stop();
        Sender.start();
        Receiver.start();
    }

    public synchronized static void addTrigger(int id, Runnable runnable) {
        if (triggers != null) triggers.put(id, runnable);
    }

    public synchronized static void removeTrigger(int id) {
        if (triggers != null) triggers.remove(id);
    }

    @Nullable
    public InetAddress getIP(@NonNull final String raw_ip) {
        IPResolver resolver = new IPResolver(raw_ip);
        Thread thread = new Thread(resolver);
        thread.start();

        try {
            thread.join();
            return resolver.getIP();

        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class IPResolver implements Runnable {
        private InetAddress ip = null;
        private String raw_ip;

        IPResolver(String raw_ip) {
            this.raw_ip = raw_ip;
        }

        @Override
        public void run() {
            try {
                // can take a lot of time for DNS lookup
                ip = InetAddress.getByName(raw_ip);

            } catch (UnknownHostException e) {
                Log.d("LOGTAG", "unable to get ip from string: " + raw_ip);
                e.printStackTrace();
            }
        }

        @Nullable
        public InetAddress getIP() {
            return ip;
        }
    }
}
