package com.lastutf445.home2.network;

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
    private static SparseArray<SyncProvider> local;

    private volatile static BufferedReader tIn;
    private static DatagramSocket uSocket;
    private static PrintWriter tOut;
    private static Socket tSocket;
    private static Thread thread;
    private static long tAlive;

    /** RETURN CODES
     *  0 - Unknown exception
     *  1 - Sent successfully
     *  2 - External address is undefined
     *  3 - No Internet connection
     *  4 - Encryption error
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
                    long time = Calendar.getInstance().getTimeInMillis();
                    long last = current.getLastAccess();

                    switch (current.getGroup()) {
                        case Sync.SYNC_DASHBOARD:
                            if (!DataLoader.getBoolean("SyncDashboard", false)) continue;
                            if (last + DataLoader.getInt("SyncDashboardInterval", 5000) > time) continue;
                            break;
                        case Sync.SYNC_MESSAGES:
                            if (!DataLoader.getBoolean("SyncMessages", false)) continue;
                            if (last + DataLoader.getInt("SyncMessagesInterval", 800) > time) continue;
                            break;
                        case Sync.SYNC_NOTIFICATIONS:
                            if (!DataLoader.getBoolean("SyncNotifications", false)) continue;
                            if (last + DataLoader.getInt("SyncNotificationsInterval", 1000) > time) continue;
                            break;
                        default:
                            if (last + 1000 > time) continue;
                            break;
                    }

                    if (current.getUseMasterConnectionOnly() && !isMasterConnectionUsed()) {
                        Log.d("LOGTAG", "requires master-connection for " + current.getQuery().toString());
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
                        // TODO: polite sleep

                        Thread.sleep(
                                500
                                //DataLoader.getInt("SyncSleepTime", 500)
                        );

                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }
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

                if (provider.getBrodcast()) query.put("ip", "broadcast");
                else if (provider.getIP() != null) {
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

                    tOut.write(query.toString() + "\n");
                    tOut.flush();
                    return 1;
                }

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
                return 0;
            }
        }

        InetAddress address = provider.getBrodcast() ? Sync.getBroadcast() : provider.getIP();
        JSONObject query = provider.getQuery();
        int port = provider.getPort();

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

        try {
            tSocket = new Socket(ip, port);
            tSocket.setSoTimeout(500);

            tOut = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(tSocket.getOutputStream())
                    ),
                    true
            );

            tIn = new BufferedReader(new InputStreamReader(tSocket.getInputStream()));
            tAlive = time + 8000;
            setupReceiver();
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
        if (tSocket == null) return;

        try {
            tSocket.close();
            tSocket = null;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void setTAlive(long tAlive) {
        Sender.tAlive = tAlive;
    }

    public synchronized static void setupReceiver() {
        Receiver.settIn(tIn);
    }

    public synchronized static boolean isMasterConnectionUsed() {
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

    public synchronized static void start() {
        if (thread != null && thread.isAlive()) return;

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
        thread = null;
    }
}
