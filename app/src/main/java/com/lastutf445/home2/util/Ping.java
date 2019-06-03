package com.lastutf445.home2.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

public class Ping extends SyncProvider {

    private WeakReference<Handler> weakHandler;
    private volatile boolean finished;
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
    public boolean isWaiting() {
        return !UserLoader.isAuthenticated();
    }

    @Override
    public void onPostPublish(int statusCode) {
        Log.d("LOGTAG", "PING: HEARTBEAT");

        switch (statusCode) {
            case 0:
                //finish(R.string.unexpectedErrorSmall);
                finish(R.string.disconnectedSmall);
                break;
            case 1:
                if (attempts-- <= 1) {
                    finish(R.string.unreachable);
                }
                break;
            case 2:
                finish(R.string.masterServerRequired);
                break;
            case 3:
                finish(R.string.disconnectedSmall);
                break;
            case 4:
                // TODO: think about it
                finish(R.string.encryptionErrorSmall);
                break;

        }
    }

    @Override
    public void onReceive(@NonNull JSONObject data) {
        try {
            if (data.has("status") && data.getInt("status") == Sync.PONG) {
                finish(R.string.reachable);
                return;
            }

            Log.d("LOGTAG", "PING: msg sent");

        } catch (JSONException e) {
            //e.printStackTrace();
        }

        finish(R.string.unreachable);
    }

    public void finish(int status) {
        if (finished) return;
        finished = true;

        Sync.removeSyncProvider(Sync.PROVIDER_PING);
        Handler handler = weakHandler.get();

        if (handler == null) {
            return;
        }

        Message msg = handler.obtainMessage(-1);
        Bundle msgData = new Bundle();
        msgData.putInt("status", status);

        msg.setData(msgData);
        handler.sendMessage(msg);

    }
}
