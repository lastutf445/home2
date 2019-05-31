package com.lastutf445.home2.network;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.lastutf445.home2.loaders.CryptoLoader;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;

public class Sender {

    private static final SparseArray<SyncProvider> syncing = Sync.getSyncing();
    private static final HashSet<Integer> removed = Sync.getRemoved();
    private volatile static WeakReference<Handler> subscriber;
    private static SparseArray<SyncProvider> local;
    private static Hello hello;

    private volatile static BufferedReader tIn;
    private volatile static long tAlive, kAlive = 0;

    private static PrintWriter tOut;
    private static Socket tSocket;
    private static Thread thread;

    /** PROVIDER RETURN CODES
     * -1 - Waiting
     *  0 - Unknown exception
     *  1 - Sent successfully
     *  2 - MasterServer required
     *  3 - No Internet connection
     *  4 - Encryption error
     */

    public static void init() {
        try {
            hello = new Hello();
            syncing.put(hello.getSource(), hello);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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
                        case Sync.SYNC_PING:
                            if (last + DataLoader.getInt("SyncPingInterval", 1000) > time) continue;
                            break;
                        case Sync.SYNC_USER_DATA:
                            //just that for now
                            if (last + 1000 > time) continue;
                            break;
                        case Sync.SYNC_MODULES_STATE:
                            //just that for now
                            if (last + 1000 > time) continue;
                            break;
                        default:
                            if (last + 1000 > time) continue;
                            break;
                    }

                    if (current.isWaiting()) {
                        current.onPostPublish(-1);
                        continue;
                    }

                    current.updateLastAccess(time);
                    kAlive = time + 3000;
                    accessed = true;

                    if (Sync.getNetworkState() == 2 && DataLoader.getString("SyncHomeNetwork", "false").equals(Sync.getNetworkBSSID())) {
                        if (DataLoader.getBoolean("MasterServer", false)) {
                            current.onPostPublish(mSend(current, time));

                        } else {
                            current.onPostPublish(2);
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
                    if (keepAliveNeeded()) {
                        kSend();
                    }

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
        int status = makeConnection(
                DataLoader.getString("MasterServerAddress", "false"),
                DataLoader.getInt("MasterServerPort", Sync.DEFAULT_PORT),
                time
        );

        if (status != 1) {
            return status;
        }

        return tSend(provider);
    }

    private synchronized static int eSend(SyncProvider provider, long time) {
        int status = makeConnection(
                DataLoader.getString("ExternalAddress", "false"),
                DataLoader.getInt("ExternalPort", Sync.DEFAULT_PORT),
                time
        );

        if (status != 1) {
            return status;
        }

        return tSend(provider);
    }

    private synchronized static int tSend(SyncProvider provider) {
        if (tOut != null && !tOut.checkError()) {
            try {
                JSONObject query = new JSONObject(provider.getQuery().toString());
                JSONObject data = query.getJSONObject("data");

                if (provider.getBroadcast()) {
                    data.put("ip", "broadcast");
                    data.put("port", provider.getPort());

                } else if (provider.getIP() != null) {
                    data.put("ip", provider.getIP().getHostAddress());
                    data.put("port", provider.getPort());
                }

                if (provider.getEncrypted()) {
                    String raw_data = data.toString();
                    String encrypted;

                    if (CryptoLoader.hasAESKey()) {
                        encrypted = CryptoLoader.AESEncrypt(raw_data);
                        query.put("aes", true);

                    } else {
                        encrypted = CryptoLoader.RSAEncrypt(raw_data);
                        query.put("rsa", true);
                    }

                    if (encrypted == null) {
                        Log.d("LOGTAG", "can't encrypt packet: " + provider.getSource());
                        return 4;
                    }

                    query.put("data", encrypted);

                } else {
                    query.put("data", data);
                }

                query.put("session", UserLoader.getSession());

                publish(-2);
                tOut.write(query.toString() + "\n");
                tOut.flush();
                return 1;

            } catch (JSONException e) {
                //e.printStackTrace();

            }
        }

        return 0;
    }

    private synchronized static void kSend() {
        long time = System.currentTimeMillis();

        if (tOut != null && !tOut.checkError() && time < tAlive) {
            kAlive = time + 3000;
            tOut.write("z\n");
            //publish(-2);
            tOut.flush();

            //Log.d("LOGTAG", "kAlive: " + kAlive);
        } else if (DataLoader.getBoolean("SyncPersistentConnection", false)) {
            int status = 0;

            if (Sync.getNetworkState() == 2 && DataLoader.getString("SyncHomeNetwork", "false").equals(Sync.getNetworkBSSID())) {
                if (DataLoader.getBoolean("MasterServer", false)) {
                    status = makeConnection(
                            DataLoader.getString("MasterServerAddress", "false"),
                            DataLoader.getInt("MasterServerPort", Sync.DEFAULT_PORT),
                            time
                    );
                }

            } else if (Sync.getNetworkState() != 0) {
                if (DataLoader.getBoolean("ExternalConnection", false)) {
                    status = makeConnection(
                            DataLoader.getString("ExternalAddress", "false"),
                            DataLoader.getInt("ExternalPort", Sync.DEFAULT_PORT),
                            time
                    );
                }
            }

            if (status != 1) {
                publish(-1);

                try {
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            } else if (hello != null) {
                hello.isWaiting = false;
            }
        }
    }

    public synchronized static int makeConnection(String ip, int port, long time) {
        if (tSocket != null && tSocket.isConnected() && tSocket.getInetAddress().getHostAddress().equals(ip) && tSocket.getPort() == port && time < tAlive) return 1;
        Log.d("LOGTAG", "makeConnection triggered");
        killConnection();

        try {
            tSocket = new Socket();
            tSocket.connect(
                    new InetSocketAddress(ip, port),
                    // TODO: move to DataLoader
                    3000
            );

            tSocket.setReuseAddress(true);
            //tSocket.setSoTimeout(1000);

            tOut = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(tSocket.getOutputStream())
                    ),
                    true
            );

            tIn = new BufferedReader(new InputStreamReader(tSocket.getInputStream()));
            ModulesLoader.onReconnect();
            UserLoader.onReconnect();
            tAlive = time + 6000;
            connectReceiver();
            //publish(-2);
            return 1;

        } catch (UnknownHostException e) {
            //e.printStackTrace();
            Log.d("LOGTAG", "Unable to resolve host");
            return 2;

        } catch (IOException e) {
            //e.printStackTrace();
            Log.d("LOGTAG", "Unable to create socket");
            return 0;
        }
    }

    public synchronized static void killConnection() {
        publish(-1);
        Log.d("LOGTAG", "killConnection()");
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
        publish(-2);
        //Log.d("LOGTAG", "tAlive: " + tAlive);
    }

    public static void connectReceiver() {
        Receiver.settIn(tIn);
    }

    public static boolean keepAliveNeeded() {
        return System.currentTimeMillis() > kAlive && UserLoader.isAuthenticated();
    }

    public static void subscribe(@NonNull Handler handler) {
        subscriber = new WeakReference<>(handler);
    }

    public static void unsubscribe() {
        if (subscriber != null) {
            subscriber.clear();
        }
    }

    public static void publish(int code) {

        /** SUBSCRIBER RETURN CODES
         *  -1 - Idle
         *  -2 - Master Server
         */

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
        thread = null;
    }

    private static class Hello extends SyncProvider {
        private boolean isWaiting = true;

        public Hello() throws JSONException {
            super(
                    Sync.PROVIDER_HELLO,
                    "hello",
                    new JSONObject(),
                    null,
                    0
            );
        }

        @Override
        public boolean isWaiting() {
            if (isWaiting || !UserLoader.isAuthenticated()) {
                return true;

            } else {
                isWaiting = true;
                return false;
            }
        }

        @Override
        public boolean getEncrypted() {
            return false;
        }
    }
}
