package com.lastutf445.home2.network;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.util.Log;

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

public class Receiver {

    private static DatagramSocket uSocket;
    private static BufferedReader tIn;
    private static Thread thread;

    private static Runnable task = new Runnable() {
        @Override
        public void run() {
            while (!thread.isInterrupted()) {
                if (Sync.getNetworkState() == 2 && DataLoader.getString("SyncHomeNetwork", "false").equals(Sync.getNetworkBSSID()) && !DataLoader.getBoolean("MasterServer", false)) {
                    uReceive();

                } else if (Sync.getNetworkState() == 0) {
                    sleep();

                } else {
                    tReceive();
                }

            }
        }
    };

    private static void tReceive() {
        if (tIn == null) {
            sleep();
            return;
        }

        try {
            String buf = tIn.readLine();
            if (buf == null) return;
            onReceived(buf.trim());

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
            e.printStackTrace();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(500);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void onReceived(String result) {
        Sender.setTAlive(Calendar.getInstance().getTimeInMillis() + 8000);
        Log.d("LOGTAG", "result: " + result);

        if (result.equals("alive")) {
            return;
        }

        try {
            JSONObject json = new JSONObject(result);
            JSONObject data = json.getJSONObject("data");
            int source = json.getInt("id");
            Sync.callProvider(source, data);

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    private static boolean setuSocket() {
        if (uSocket != null) return true;

        try {
            uSocket = new DatagramSocket(
                    DataLoader.getInt("SyncClientPort", Sync.DEFAULT_PORT)
            );

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

    public static void start() {
        thread = new Thread(task);
        thread.setPriority(2);
        thread.start();
    }

    public static void stop() {
        if (thread != null && !thread.isInterrupted()) {
            thread.interrupt();
        }

        uSocket = null;
    }
}
