package com.lastutf445.home2.network;

import android.content.ContentValues;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import com.lastutf445.home2.loaders.CryptoLoader;
import com.lastutf445.home2.loaders.DataLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

public class Receiver {

    private static StringBuffer buf = new StringBuffer();
    private static DatagramSocket uSocket;
    private static BufferedReader tIn;
    private static Thread thread;


    private static Runnable task = new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                if (Sync.getNetworkState() == 2 && DataLoader.getString("SyncHomeNetwork", "false").equals(Sync.getNetworkBSSID()) && !DataLoader.getBoolean("MasterServer", false)) {
                    uReceive();

                } else if (Sync.getNetworkState() == 0) {
                    sleep();

                } else {
                    tReceive();
                }

                if (!Thread.interrupted()) sleep();
                else break;
            }
        }
    };

    private static void tReceive() {
        if (tIn == null) {
            sleep();
            return;
        }

        try {
            String s = tIn.readLine();
            onReceived(s.trim());

        } catch (Exception e) {
            //Log.d("LOGTAG-ERROR", e.getMessage());
            sleep();
        }
    }

    private static void uReceive() {
        if (!setuSocket()) {
            sleep();
            return;
        }

        byte[] buf = new byte[2048];
        DatagramPacket p = new DatagramPacket(buf, buf.length);

        try {
            uSocket.receive(p);

            if (Sync.getLocal().equals(p.getAddress())) return;
            onReceived(new String(buf).trim());

        } catch (IOException e) {
            //e.printStackTrace();
        }

        sleep();
    }

    private static void sleep() {
        try {
            Thread.sleep(500);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void onReceived(final String result) {
        Sender.setTAlive(System.currentTimeMillis() + 8000);
        //Log.d("LOGTAG","result: " + result);

        if (result.equals("alive")) {
            return;
        }

        try {
            JSONObject json = new JSONObject(result);
            JSONObject data;

            if (json.has("aes") && json.getBoolean("aes")) {
                String raw_data = json.getString("data");
                String decrypted = CryptoLoader.AESDecrypt(raw_data);

                if (decrypted == null) {
                    Log.d("LOGTAG", "decrypt error");
                    return;
                }

                data = new JSONObject(decrypted);

            } else {
                data = json.getJSONObject("data");
            }

            int source = json.getInt("id");
            Log.d("LOGTAG", "result, id " + source + ": " + data.toString());

            new AsyncTask<Result, Void,  Void>() {
                @Override
                protected Void doInBackground(Result... results) {
                    if (results[0] != null) {
                        Sync.callProvider(results[0].getSource(), results[0].getData());
                    }

                    return null;
                }
            }.execute(new Result(source, data));

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    private static boolean setuSocket() {
        if (uSocket != null && !uSocket.isClosed()) return true;

        try {
            uSocket = new DatagramSocket(
                    DataLoader.getInt("SyncClientPort", Sync.DEFAULT_PORT)
            );

            uSocket.setSoTimeout(1000);

            //uSocket.setReuseAddress(true);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void settIn(BufferedReader tIn) {
        Receiver.tIn = tIn;
    }

    public synchronized static void start() {
        if (thread != null) return;

        thread = new Thread(task);
        thread.setName("Receiver");
        thread.setPriority(3);
        thread.start();
    }

    public synchronized static void stop() {
        if (uSocket != null && !uSocket.isClosed()) {
            uSocket.close();
        }

        if (thread != null) {
            thread.interrupt();

            try {
                thread.join();

            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        uSocket = null;
        thread = null;
        tIn = null;
    }

    private final static class Result {
        private int source;
        private JSONObject data;

        public Result(int source, JSONObject data) {
            this.source = source;
            this.data = data;
        }

        public int getSource() {
            return source;
        }

        public JSONObject getData() {
            return data;
        }
    }
}
