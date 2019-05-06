package com.lastutf445.home2.network;

import android.util.Log;
import android.util.SparseArray;

import com.lastutf445.home2.fragments.menu.MasterServer;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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

    private static DatagramSocket uSocket;
    private static BufferedReader tIn;
    private static PrintWriter tOut;
    private static Socket tSocket;

    private static Thread thread;

    /** RETURN CODES
     *  0 - Unknown exception
     *  1 - Sent successfully
     *  2 - External address is undefined
     *  3 - No Internet connection
     */

    private static Runnable task = new Runnable() {
        @Override
        public void run() {
            while (!thread.isInterrupted()) {
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

                    current.updateLastAccess(time);
                    accessed = true;

                    if (Sync.getNetworkState() == 2 && DataLoader.getString("SyncHomeNetwork", "false").equals(Sync.getNetworkBSSID())) {
                        if (DataLoader.getBoolean("MasterServer", false)) {
                            current.onPostPublish(mSend(current));

                        } else {
                            current.onPostPublish(uSend(local.valueAt(i)));
                        }

                    } else if (Sync.getNetworkState() != 0) {
                        if (DataLoader.getBoolean("ExternalConnection", false)) {
                            current.onPostPublish(eSend(local.valueAt(i)));

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
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private static int mSend(SyncProvider provider) {
        if (!makeConnection(
                DataLoader.getString("MasterServerAddress", "false"),
                DataLoader.getInt("MasterServerPort", Sync.DEFAULT_PORT))
        ) return 0;

        return tSend(provider);
    }

    private static int eSend(SyncProvider provider) {
        if (!makeConnection(
                DataLoader.getString("ExternalAddress", "false"),
                DataLoader.getInt("ExternalPort", Sync.DEFAULT_PORT))
        ) return 0;

        return tSend(provider);
    }

    private static int tSend(SyncProvider provider) {
        if (tOut != null) {
            JSONObject query = provider.getQuery();

            try {
                query.put("port", DataLoader.getInt("SyncClientPort", Sync.DEFAULT_PORT));

            } catch (JSONException e) {
                //e.printStackTrace();
            }

            tOut.write(query.toString());
            return 1;
        }

        return 0;
    }

    private static int uSend(SyncProvider provider) {
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
            //uSocket.setBroadcast(provider.getBrodcast());
            uSocket.send(p);

            //uSocket.setBroadcast(false);
            return 1;

        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean makeConnection(String ip, int port) {
        Log.d("LOGTAG", "makeConnection triggered");
        if (tSocket != null && tSocket.isConnected() && tSocket.getInetAddress().getHostAddress().equals(ip) && tSocket.getPort() == port) return true;
        killConnection();

        try {
            tSocket = new Socket(ip, port);

            tSocket.setSoTimeout(
                    DataLoader.getInt("MasterServerSoTimeout", 1000)
            );

            tOut = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(tSocket.getOutputStream())
                    ),
                    true
            );

            tIn = new BufferedReader(new InputStreamReader(tSocket.getInputStream()));
            Receiver.settIn(tIn);

        } catch (UnknownHostException e) {
            //e.printStackTrace();
            Log.d("LOGTAG", "Unable to resolve host");

        } catch (IOException e) {
            //e.printStackTrace();
            Log.d("LOGTAG", "Unable to create socket");
        }

        return false;
    }

    public static void killConnection() {
        if (tSocket == null) return;

        try {
            tSocket.close();
            tSocket = null;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void start() {
        thread = new Thread(task);
        thread.setPriority(1);
        thread.start();
    }

    public static void stop() {
        if (thread != null && !thread.isInterrupted()) {
            thread.interrupt();
        }

        killConnection();
    }
}
