package com.lastutf445.home2.network;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.lastutf445.home2.fragments.menu.MasterServer;
import com.lastutf445.home2.loaders.CryptoLoader;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;

public class Sender {

    private static final SparseArray<SyncProvider> syncing = Sync.getSyncing();
    private static final HashSet<Integer> removed = Sync.getRemoved();
    private volatile static WeakReference<Handler> subscriber;
    private static SparseArray<SyncProvider> local;

    private volatile static BufferedReader tIn;
    private volatile static long tAlive;

    private static DatagramSocket uSocket;
    private static PrintWriter tOut;
    private static Socket tSocket;
    private static Thread thread;

    /** PROVIDER RETURN CODES
     *  0 - Unknown exception
     *  1 - Sent successfully
     *  2 - External address is undefined
     *  3 - No Internet connection
     *  4 - Encryption error
     *  5 - MasterServer required
     */

    /** SUBSCRIBER RETURN CODES
     *  -1 - Disconnected
     *  -2 - Home network
     *  -3 - Master Server
     *  -4 - Idle
     */

    private static Runnable task = new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                boolean accessed = false;

                synchronized (syncing) {
                    synchronized (removed) {
                        for (int toRemove: removed) {
                            syncing.remove(toRemove);
                        }

                        removed.clear();
                    }

                    local = syncing.clone();
                }

                for (int i = 0; i < local.size(); ++i) {
                    SyncProvider current = local.valueAt(i);
                    long time = System.currentTimeMillis();
                    long last = current.getLastAccess();

                    switch (current.getGroup()) {
                        case Sync.SYNC_DASHBOARD:
                            if (!DataLoader.getBoolean("SyncDashboard", false)) continue;
                            if (last + DataLoader.getInt("SyncDashboardInterval", 5000) > time) continue;
                            break;
                        case Sync.SYNC_MESSAGES:
                            if (!DataLoader.getBoolean("SyncMessages", false)) continue;
                            if (last + DataLoader.getInt("SyncMessagesInterval", 500) > time) continue;
                            break;
                        case Sync.SYNC_NOTIFICATIONS:
                            if (!DataLoader.getBoolean("SyncNotifications", false)) continue;
                            if (last + DataLoader.getInt("SyncNotificationsInterval", 1000) > time) continue;
                            break;
                        case Sync.SYNC_PING:
                            if (last + DataLoader.getInt("SyncPingInterval", 1000) > time) continue;
                            break;
                        default:
                            if (last + 1000 > time) continue;
                            break;
                    }

                    if (current.getUseMasterConnectionOnly() && !isMasterConnectionUsed()) {
                        Log.d("LOGTAG", "requires master-connection for " + current.getQuery().toString());
                        current.onPostPublish(5);
                        continue;
                    }

                    current.updateLastAccess(time);
                    accessed = true;

                    if (Sync.getNetworkState() == 2 && DataLoader.getString("SyncHomeNetwork", "false").equals(Sync.getNetworkBSSID())) {
                        if (DataLoader.getBoolean("MasterServer", false)) {
                            current.onPostPublish(mSend(current, time));

                        } else {
                            current.onPostPublish(uSend(local.valueAt(i)));
                        }

                    } else if (Sync.getNetworkState() != 0) {
                        if (DataLoader.getBoolean("ExternalConnection", false)) {
                            current.onPostPublish(eSend(local.valueAt(i), time));

                        } else {
                            current.onPostPublish(2);
                        }

                    } else {
                        current.onPostPublish(3);
                    }
                }

                if (!accessed) {
                    try {
                        Thread.sleep(250);

                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }

            publish(-1);
        }
    };

    private synchronized static int mSend(SyncProvider provider, long time) {
        if (!makeConnection(
                DataLoader.getString("MasterServerAddress", "false"),
                DataLoader.getInt("MasterServerPort", Sync.DEFAULT_PORT),
                time
        )) return 0;

        return tSend(provider);
    }

    private synchronized static int eSend(SyncProvider provider, long time) {
        if (!makeConnection(
                DataLoader.getString("ExternalAddress", "false"),
                DataLoader.getInt("ExternalPort", Sync.DEFAULT_PORT),
                time
        )) return 0;

        return tSend(provider);
    }

    private synchronized static int tSend(SyncProvider provider) {
        if (tOut != null) {
            try {
                JSONObject query = new JSONObject(provider.getQuery().toString());

                if (provider.getBrodcast()) {
                    query.put("ip", "broadcast");

                } else if (provider.getIP() != null) {
                    query.put("ip", provider.getIP().getHostAddress());
                }

                query.put("port", provider.getPort());

                if (provider.getEncrypted()) {
                    String data = query.getJSONObject("data").toString();
                    String encrypted;

                    if (CryptoLoader.hasAESKey()) {
                        encrypted = CryptoLoader.AESEncrypt(data);
                        query.put("aes", true);

                    } else {
                        encrypted = CryptoLoader.RSAEncrypt(data);
                        query.put("rsa", true);
                    }

                    if (encrypted == null) {
                        Log.d("LOGTAG", "can't encrypt packet: " + provider.getSource());
                        return 4;
                    }

                    query.put("session", UserLoader.getSession());
                    query.put("data", encrypted);

                }

                publish(-3);
                tOut.write(query.toString() + "\n");
                tOut.flush();
                return 1;

            } catch (JSONException e) {
                //e.printStackTrace();
            }
        }

        return 0;
    }

    private synchronized static int uSend(SyncProvider provider) {
        if (uSocket == null) {
            try {
                uSocket = new DatagramSocket();

            } catch (IOException e) {
                e.printStackTrace();
                publish(-1);
                return 0;
            }
        }

        InetAddress address = provider.getBrodcast() ? Sync.getBroadcast() : provider.getIP();
        JSONObject query = provider.getQuery();
        int port = provider.getPort();

        publish(-2);

        try {
            query.put("port", DataLoader.getInt("SyncClientPort", Sync.DEFAULT_PORT));

        } catch (JSONException e) {
            //e.printStackTrace();
        }

        byte[] msg = query.toString().getBytes();
        DatagramPacket p = new DatagramPacket(msg, msg.length, address, port);

        try {
            uSocket.send(p);
            return 1;

        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public synchronized static boolean makeConnection(String ip, int port, long time) {
        if (tSocket != null && tSocket.isConnected() && tSocket.getInetAddress().getHostAddress().equals(ip) && tSocket.getPort() == port && time < tAlive) return true;
        Log.d("LOGTAG", "makeConnection triggered");
        killConnection();
        publish(-4);

        try {
            tSocket = new Socket(ip, port);
            tSocket.setReuseAddress(true);
            tSocket.setSoTimeout(1000);

            tOut = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(tSocket.getOutputStream())
                    ),
                    true
            );

            tIn = new BufferedReader(new InputStreamReader(tSocket.getInputStream()));
            tAlive = time + 8000;
            connectReceiver();
            publish(-3);
            return true;

        } catch (UnknownHostException e) {
            //e.printStackTrace();
            Log.d("LOGTAG", "Unable to resolve host");

        } catch (IOException e) {
            //e.printStackTrace();
            Log.d("LOGTAG", "Unable to create socket");
        }

        return false;
    }

    public synchronized static void killConnection() {
        publish(-1);
        if (tSocket == null) return;

        try {
            tSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        tSocket = null;
    }

    public static void setTAlive(long tAlive) {
        Sender.tAlive = tAlive;
    }

    public synchronized static void connectReceiver() {
        Receiver.settIn(tIn);
    }

    public static boolean isMasterConnectionUsed() {
        if (Sync.getNetworkState() == 0) {
            return false;
        }

        else if (Sync.getNetworkBSSID().equals(DataLoader.getString("SyncHomeNetwork", "false"))
                && DataLoader.getBoolean("MasterServer", false)) {
            return true;
        }

        else if (DataLoader.getBoolean("ExternalConnection", false)) {
            return true;
        }

        return false;
    }

    public static void subscribe(@NonNull Handler handler) {
        subscriber = new WeakReference<>(handler);
    }

    public static void unsubscribe() {
        if (subscriber != null) {
            subscriber.clear();
        }
    }

    private static void publish(int code) {
        //Log.d("LOGTAG", "publish: " + code);
        if (subscriber == null) return;

        Handler handler = subscriber.get();

        if (handler != null) {
            handler.sendEmptyMessage(code);
        }
    }

    public synchronized static void start() {
        if (thread != null) return;

        thread = new Thread(task);
        thread.setName("Sender");
        thread.setPriority(3);
        thread.start();
    }

    public synchronized static void stop() {
        if (thread != null) {
            thread.interrupt();

            try {
                thread.join();

            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        killConnection();
        publish(0);
        thread = null;
    }
}
