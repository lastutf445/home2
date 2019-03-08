package lastutf445.android.com.home2;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.lang.Thread.sleep;

public class Sync {

    private static Thread syncReceiver;
    private static Thread syncSender;

    private static DatagramSocket socket;
    private static int port = 44501;
    private static int sleep = 5;

    // {op: {id: listener}}
    private static HashMap<String, HashMap<Integer, SyncListener>> subs = new HashMap<>();

    // {nodeId: syncQuery}
    private static HashMap<Integer, JSONObject> pubs = new HashMap<>();


    private static class wHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    private static Runnable receiver = new Runnable() {
        @Override
        public void run() {
            try {
                socket = new DatagramSocket(port);

            } catch (SocketException e) {
                e.printStackTrace();
            }

            while (!socket.isClosed() && !Thread.interrupted()) {
                try {
                    byte[] buf = new byte[2048];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    JSONObject obj = new JSONObject(new String(buf));
                    JSONObject data = obj.getJSONObject("data");

                    Iterator<String> i = data.keys();

                    while (i.hasNext()) {
                        String id = i.next();
                        if (hasSubscribers(id)) {
                            for (SyncListener listener: getSubscribers(id).values()) {
                                listener.onReceive(
                                        data.getJSONArray(id),
                                        obj.getInt("node")
                                );
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            socket.close();
        }
    };

    private static Runnable sender = new Runnable() {
        @Override
        public void run() {
            HashMap<Integer, JSONObject> queries = new HashMap<>(pubs);

            while (!Thread.interrupted()) {
                try {
                    for (Map.Entry<Integer, JSONObject> i : queries.entrySet()) {
                        try {
                            send(i.getValue().toString());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    sleep(sleep);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void send(String msg) throws IOException {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] data = msg.getBytes();

            DatagramPacket packet = new DatagramPacket(
                    data, data.length, getBroadcastAddress(), port
            );

            socket.send(packet);
        }

        private InetAddress getBroadcastAddress() throws UnknownHostException {
            WifiManager wifi = (WifiManager) MainActivity.getAppContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcp = wifi.getDhcpInfo();

            if(dhcp == null) {
                return InetAddress.getByName("255.255.255.255");
            }

            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];

            for (int k = 0; k < 4; k++) {
                quads[k] = (byte) (broadcast >> (k * 8));
            }

            return InetAddress.getByAddress(quads);
        }
    };

    public static void subscribe(String op, int id, SyncListener listener) {
        if (!subs.containsKey(op)) {
            subs.put(op, new HashMap<Integer, SyncListener>());
        }

        subs.get(op).put(id, listener);
    }

    public static void unsubscribe(String op, int id) {
        if (subs.containsKey(op) && subs.get(op).containsKey(id)) {
            subs.get(op).remove(id);
        }
    }

    public static boolean hasSubscribers(String op) {
        return subs.containsKey(op) && !subs.get(op).isEmpty();
    }

    public static boolean isSubscribed(String op, int id) {
        return subs.containsKey(op) && subs.get(op).containsKey(id);
    }

    public static HashMap<Integer, SyncListener> getSubscribers(String op) {
        return Sync.subs.get(op);
    }

    public static void publish(int id, JSONObject query) {
        pubs.put(id, query);
    }

    public static void unpublish(int id) {
        pubs.remove(id);
    }

    public synchronized static boolean start() {
        if (!syncReceiver.isAlive()) {
            syncReceiver = new Thread(receiver);
            syncReceiver.setDaemon(true);
            syncReceiver.start();
        }

        if (syncSender != null && syncSender.isAlive()) {
            return false;
        }

        syncSender = new Thread(sender);
        syncSender.start();

        return syncSender.isAlive();
    }

    public synchronized static boolean reset() {
        if (stop() && start()) {
            return true;
        }

        return false;
    }

    public synchronized static boolean stop() {
        Thread dummy = syncSender;
        syncSender = null;

        if (dummy != null) {
            dummy.interrupt();
        }

        return true;
    }

}
