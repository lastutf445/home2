package com.lastutf445.home2.network;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.util.Log;

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

public class Receiver {

    private static StringBuffer buf = new StringBuffer();
    private volatile static BufferedReader tIn;
    private static DatagramSocket uSocket;
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

            Log.d("LOGTAG", "RECEIVER: bye-bye");
        }
    };

    private synchronized static void tReceive() {
        if (tIn == null) {
            sleep();
            return;
        }

        try {
            int c;
            boolean reading = true;

            while ((c = tIn.read()) != -1 && buf.length() <= 1024) {
                if ((char) c != '\n') {
                    buf.append((char) c);

                } else {
                    reading = false;
                    break;
                }

                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (buf.length() >= 1024) {
                buf = new StringBuffer();
            }

            if (reading) return;

            String s = new String(buf);
            onReceived(s.trim());

            buf = new StringBuffer();

        } catch (Exception e) {
            //Log.d("LOGTAG-ERROR", e.getMessage());
            sleep();
        }
    }

    private synchronized static void uReceive() {
        if (!setuSocket()) {
            sleep();
            return;
        }

        byte[] buf = new byte[2048];
        DatagramPacket p = new DatagramPacket(buf, buf.length);

        try {
            //Log.d("LOGTAG", "am i interrupted? " + (thread.isInterrupted() ? "yes" : "no"));
            uSocket.receive(p);

            if (Sync.getLocal().equals(p.getAddress())) return;
            onReceived(new String(buf).trim());

        } catch (IOException e) {
            //e.printStackTrace();
        }

        sleep();
    }

    private synchronized static void sleep() {
        try {
            Thread.sleep(500);

        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private synchronized static void onReceived(String result) {
        Sender.setTAlive(Calendar.getInstance().getTimeInMillis() + 8000);
        //Log.d("LOGTAG", "am i interrupted? " + (thread.isInterrupted() ? "yes" : "no"));
        //Log.d("LOGTAG", "result: " + result);

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
            Sync.callProvider(source, data);

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    private synchronized static boolean setuSocket() {
        if (uSocket != null) return true;

        try {
            uSocket = new DatagramSocket(
                    DataLoader.getInt("SyncClientPort", Sync.DEFAULT_PORT)
            );

            uSocket.setSoTimeout(500);

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
        if (thread != null && thread.isAlive()) return;

        thread = new Thread(task);
        thread.setName("Receiver");
        thread.setPriority(3);
        thread.start();

        Sender.setupReceiver();
    }

    public synchronized static void stop() {
        if (thread != null) {
            thread.interrupt();

            if (uSocket != null) {
                uSocket.close();
            }

            if (tIn != null) {
                try {
                    tIn.close();
                    tIn = null;

                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }

            uSocket = null;
            tIn = null;

            try {
                thread.join();
//
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        thread = null;
    }
}
