package com.lastutf445.home2.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.network.Sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

public class Ping extends SyncProvider {

    private WeakReference<Handler> weakHandler;
    private int attempts;

    public Ping(@NonNull InetAddress ip, int port) throws JSONException {
        super(
                Sync.PROVIDER_PING,
                "ping",
                new JSONObject(),
                ip,
                port
        );

        attempts = DataLoader.getInt("SyncPingAttempts", 3);
        setGroup(Sync.SYNC_PING);
    }

    public void setHandler(@NonNull Handler handler) {
        this.weakHandler = new WeakReference<>(handler);
    }

    @Override
    public void onPostPublish(int statusCode) {
        if (statusCode != 1) return;
        Log.d("LOGTAG", "PING: HEARTBEAT");
        if (attempts-- > 1) return;

        Sync.removeSyncProvider(Sync.PROVIDER_PING);
        Handler handler = weakHandler.get();

        if (handler != null) {
            handler.sendEmptyMessage(-1);
        }
    }

    @Override
    public void onReceive(JSONObject data) {
        Handler handler = weakHandler.get();

        if (handler == null) {
            return;
        }

        Message msg = handler.obtainMessage(-1);
        Bundle msgData = new Bundle();

        try {
            if (data.has("success") && data.getBoolean("success")) {
                Sync.removeSyncProvider(Sync.PROVIDER_PING);
                msgData.putBoolean("success", true);

            } else {
                msgData.putBoolean("success", false);
            }

            Log.d("LOGTAG", "PING: msg sent");

            msg.setData(msgData);
            handler.sendMessage(msg);

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }
}
