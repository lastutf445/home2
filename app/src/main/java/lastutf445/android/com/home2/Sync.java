package lastutf445.android.com.home2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class Sync {

    /**
     * INIT SYNCHRONIZATION MAGIC
     * update network status
     */

    private static ConnectivityManager connectivityManager = ((ConnectivityManager) MainActivity.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE));
    private static WifiManager wifiManager = ((WifiManager) MainActivity.getAppContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE));

    private static boolean networkStateCheckerEnabled = false;
    private static InetAddress broadcastAddress;
    private static String networkBSSID = null;
    private static int networkState = 0;

    public static void init() {
        if (networkStateCheckerEnabled) return;

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        connectivityManager.registerNetworkCallback(
                builder.build(),
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                        super.onCapabilitiesChanged(network, networkCapabilities);
                        updateNetworkState();
                    }

                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        updateNetworkState();
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        updateNetworkState();
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);
                        updateNetworkState();
                    }
                }
        );

        updateNetworkState();
        networkStateCheckerEnabled = true;
    }

    private synchronized static void setNetworkState(int mode, String bssid) {
        Log.d("LOGTAG", mode + " - networkstate, " + (bssid != null ? bssid : "non-bssid"));
        networkState = mode;
        networkBSSID = bssid;
    }

    private static InetAddress getBroadcastAddress() throws UnknownHostException {
        DhcpInfo dhcp = wifiManager.getDhcpInfo();

        if(dhcp == null) {
            return InetAddress.getByName("255.255.255.255");
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];

        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }

        return InetAddress.getByAddress(quads);

    }

    private synchronized static void updateNetworkState() {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (networkInfo != null && wifiInfo != null && networkInfo.isConnected()) {
            setNetworkState(2, wifiInfo.getBSSID());

            try {
                broadcastAddress = getBroadcastAddress();

            } catch (UnknownHostException e) {
                broadcastAddress = null;
                e.printStackTrace();
            }

            return;
        }

        networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            setNetworkState(1, null);
            return;
        }

        setNetworkState(0, null);
    }

    public static String getNetworkBSSID() {
        return networkBSSID;
    }

    public static int getNetworkState() {
        return networkState;
    }

    /**
     * SYNCHRONIZATION THREADS
     * get and send data
     */

    private static Thread receiver;
    private static Thread sender;

    private static HashMap<Integer, SyncProvider> subs = new HashMap<>();
    private static HashMap<Integer, SyncProvider> pubs = new HashMap<>();

    private static Integer port;
    private static Integer sleep;

    public synchronized static void start() {
        port = Data.getInt("SyncClientPort", 44501);
        sleep = Data.getInt("SyncSleepTime", 5000);

        /** RETURN CODES
         *  0 - Unknown exception
         *  1 - Sent successfully
         *  2 - Can't send broadcast being outside the home network
         *  3 - External address is undefined
         *  4 - Wrong destination
         */

        if (receiver == null || !receiver.isAlive()) {
            receiver = new Thread(new Runnable() {

                private DatagramSocket socket;

                @Override
                public void run() {
                    try {
                        socket = new DatagramSocket(port);

                    } catch (SocketException e) {
                        e.printStackTrace();
                    }

                    while (socket != null && !socket.isConnected() && !Thread.interrupted()) {
                        try {
                            byte[] buf = new byte[2048];
                            DatagramPacket packet = new DatagramPacket(buf, buf.length);
                            socket.receive(packet);

                            JSONObject obj = new JSONObject((new String(buf)).trim());
                            int nodeSerial = obj.getInt("nodeSerial");
                            JSONObject data = obj.getJSONObject("data");

                            if (subs.containsKey(nodeSerial)) {
                                subs.get(nodeSerial).onReceive(data, nodeSerial);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();

                        } catch (JSONException e) {
                            e.printStackTrace();

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }

                    socket.close();
                }
            });

            //receiver.setDaemon(true);
            receiver.start();
        }

        if (sender == null || !sender.isAlive()) {
            sender = new Thread(new Runnable() {
                private HashMap<Integer, SyncProvider> local;

                @Override
                public void run() {
                    while (!Thread.interrupted()) {
                        local = new HashMap<>(pubs);

                        for (SyncProvider i: local.values()) {
                            try {
                                JSONObject obj = i.getQuery();
                                i.onPublish(send(obj));

                            } catch (IOException e) {
                                e.printStackTrace();
                                i.onPublish(0);

                            } catch (JSONException e) {
                                e.printStackTrace();
                                i.onPublish(0);

                            } catch (NullPointerException e) {
                                e.printStackTrace();
                                i.onPublish(0);
                            }
                        }

                        try {
                            sleep(sleep);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                private int send(JSONObject query) throws IOException, JSONException {
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet;

                    int port = query.has("port") ? query.getInt("port") : Sync.port;
                    query.put("port", Sync.port);

                    byte[] msg = query.toString().getBytes();

                    if (query.has("broadcast")) {
                        if (!networkBSSID.equals(Data.getString("SyncHomeNetwork", "false"))) {
                            return 2;
                        }

                        socket.setBroadcast(true);

                        packet = new DatagramPacket(
                                msg, msg.length, broadcastAddress, port
                        );

                        socket.send(packet);
                    }

                    else if (query.has("address")) {
                        if (networkBSSID.equals(Data.getString("SyncHomeNetwork", "false"))) {
                            packet = new DatagramPacket(
                                    msg, msg.length, InetAddress.getByName(query.getString("address")), port
                            );
                        }
                        else if (Data.getString("SyncExternalAddress", null) != null) {
                            packet = new DatagramPacket(
                                    msg, msg.length, InetAddress.getByName(Data.getString("SyncExternalAddress", null)), port
                            );
                        }
                        else {
                            socket.close();
                            return 3;
                        }

                        socket.send(packet);
                    }

                    else {
                        socket.close();
                        return 4;
                    }

                    socket.close();
                    return 1;
                }

            });
            sender.start();
        }
    }

    public synchronized static void restart() {
        Log.d("LOGTAG", "SYNC RESTART");

        stop();
        start();
    }

    public synchronized static void stop() {
        if (receiver != null && receiver.isAlive()) {
            receiver.interrupt();
        }

        if (sender != null && sender.isAlive()) {
            sender.interrupt();
        }
    }

    public synchronized static void subscribe(int id, SyncProvider provider) {
        subs.put(id, provider);
    }

    public synchronized static void publish(int id, SyncProvider provider) {
        pubs.put(id, provider);
    }

    public synchronized static void unsubscribe(int id) {
        subs.remove(id);
    }

    public synchronized static void unpublish(int id) {
        pubs.remove(id);
    }
}
