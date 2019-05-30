package com.lastutf445.home2.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;

public class Sync {

    public static final int DEFAULT_PORT = 44501;

    public static final int SYNC_PING = 0;
    public static final int SYNC_USER_DATA = 1;
    public static final int SYNC_MODULES_STATE = 2;

    public static final int PROVIDER_DASHBOARD = 0;
    public static final int PROVIDER_MESSAGES = -1;
    public static final int PROVIDER_NOTIFICATIONS = -2;
    public static final int PROVIDER_DISCOVERER = -3;
    public static final int PROVIDER_USER_DATA = -4;
    public static final int PROVIDER_MODULE_EDIT_REQUEST = -5;
    public static final int PROVIDER_GET_PUBLIC_KEY = -6;
    public static final int PROVIDER_CREDENTIALS = -7;
    public static final int PROVIDER_EDITOR = -8;
    public static final int PROVIDER_PING = -9;
    public static final int PROVIDER_SYNC_MODULES_STATE = -10;
    public static final int PROVIDER_HELLO = -11;

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
    public static final int UNKNOWN_MODULE = 13;
    public static final int UPDATE = 14;
    public static final int SYNC_USER_DATA_EVENT = 15;
    public static final int SYNC_MODULES_STATE_EVENT = 16;
    public static final int SYNC_USER_DATA_FAILED_EVENT = 17;
    public static final int SYNC_MODULES_STATE_FAILED_EVENT = 18;
    public static final int SYNC_SUBSCRIBE = 19;
    public static final int SYNC_UNSUBSCRIBE = 20;

    private static ConnectivityManager connectivityManager;
    private static WifiManager wifiManager;

    private static SparseArray<Runnable> triggers = new SparseArray<>();
    private static String networkBSSID = null;
    private static int networkState = 0;

    private static final SparseArray<SyncProvider> syncing = new SparseArray<>();
    private static final HashSet<Integer> removed = new HashSet<>();

    public static void init() {
        Log.d("LOGTAG", "sync initialization...");

        connectivityManager = (ConnectivityManager) DataLoader.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) DataLoader.getAppContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        connectivityManager.registerNetworkCallback(
                builder.build(),
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                        updateNetworkState2();
                    }

                    @Override
                    public void onAvailable(Network network) {
                        updateNetworkState2();
                    }

                    @Override
                    public void onUnavailable() {
                        updateNetworkState2();
                    }

                    @Override
                    public void onLost(Network network) {
                        updateNetworkState2();
                    }
                }
        );

        Sender.init();
        start();
    }

    public static void updateNetworkState2() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);

        if (capabilities == null) {
            setNetworkState(0, null);
            Sender.publish(-1);

        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            setNetworkState(1, null);

        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            setNetworkState(2, wifiInfo.getBSSID());

        } else {
            setNetworkState(0, null);
            Sender.publish(-1);
        } // todo: ethernet or vpn?

        callTriggers();
    }

    public static void callTriggers() {
        //Log.d("LOGTAG", "onupdatenetworkstate triggers.size() = " + triggers.size());
        for (int i = 0; i < triggers.size(); ++i) {
            (new Thread(triggers.valueAt(i))).start();
        }
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

    public synchronized static String getNetworkBSSID() {
        return networkBSSID;
    }

    public static int getNetworkState() {
        return networkState;
    }

    public static SparseArray<SyncProvider> getSyncing() {
        return syncing;
    }

    public static HashSet<Integer> getRemoved() {
        return removed;
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
}
