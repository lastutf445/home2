package com.lastutf445.home2.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.network.Sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class GlobalPing extends Thread {
    private WeakReference<Handler> weakHandler;
    private volatile boolean aborted = false;
    @Nullable
    private InetAddress ip;
    private int port;

    private int attempts, interval;

    private volatile BufferedReader in;
    private volatile PrintWriter out;
    @Nullable
    private volatile Socket sock;

    public GlobalPing(@NonNull Handler handler, @Nullable InetAddress ip, int port) {
        interval = DataLoader.getInt("SyncPingInterval", 1000);
        attempts = DataLoader.getInt("SyncPingAttempts", 3);
        weakHandler = new WeakReference<>(handler);
        this.port = port;
        this.ip = ip;
    }

    @Override
    public void run() {
        Handler handler = weakHandler.get();

        if (handler != null) {
            handler.sendEmptyMessage(-2);
        }

        while (!aborted && attempts-- > 0) {
            makeConnection();
            Log.d("LOGTAG", "PING: HEARTBEAT");
            cycle();
        }

        abort();
        killConnection();
    }

    private void cycle() {
        if (in == null) {
            return;
        }

        out.write("{\"id\":0, \"act\":\"globalPing\",\"data\":{}}\n");
        out.flush();

        try {
            String buf = in.readLine();

            try {
                JSONObject json = new JSONObject(buf);
                onReceive(json.getJSONObject("data"));

            } catch (JSONException e) {
                //e.printStackTrace();
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }

        sleep();
    }

    private void onReceive(@NonNull JSONObject data) {
        Handler handler = weakHandler.get();

        if (handler == null) {
            return;
        }

        Message msg = handler.obtainMessage(-1);
        Bundle msgData = new Bundle();

        try {
            if (data.has("status") && data.getInt("status") == Sync.PONG) {
                msgData.putBoolean("success", true);

            } else {
                msgData.putBoolean("success", false);
            }

            Log.d("LOGTAG", "PING: msg sent");

            msg.setData(msgData);
            handler.sendMessage(msg);
            weakHandler.clear();
            abort();

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    public void abort() {
        if (aborted) return;
        aborted = true;

        Handler handler = weakHandler.get();

        if (handler != null) {
            handler.sendEmptyMessage(-1);
        }
    }

    private void sleep() {
        try {
            sleep(interval);

        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }

    private void makeConnection() {
        if (sock != null && sock.isConnected() && sock.getInetAddress().getHostAddress().equals(ip.getHostAddress()) && sock.getPort() == port) return;
        Log.d("LOGTAG", "PING: makeConnection triggered");
        killConnection();

        try {
            sock = new Socket();
            sock.connect(
                    new InetSocketAddress(ip, port),
                    // TODO: move to DataLoader
                    1000
            );

            sock.setReuseAddress(true);
            sock.setSoTimeout(1000);

            out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(sock.getOutputStream())
                    ),
                    true
            );

            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

        } catch (UnknownHostException e) {
            //e.printStackTrace();
            Log.d("LOGTAG", "PING: Unable to resolve host");
            sleep();

        } catch (IOException e) {
            //e.printStackTrace();
            Log.d("LOGTAG", "PING: Unable to create socket");
            sleep();
        }
    }

    public void killConnection() {
        if (sock == null) return;

        try {
            sock.close();
            sock = null;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
