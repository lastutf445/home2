package com.lastutf445.home2.util;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.lastutf445.home2.network.Sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

public class Ping extends SyncProvider {

    protected boolean useMasterServerOnly = false;
    private WeakReference<Handler> weakHandler;

    public Ping(@NonNull InetAddress ip, int port) throws JSONException {
        super(
                Sync.PROVIDER_PING,
                "ping",
                new JSONObject(),
                ip,
                port
        );
    }

    public void setWeakHandler(@NonNull Handler handler) {
        this.weakHandler = new WeakReference<>(handler);
    }

    public void setUseMasterServerOnly(boolean useMasterServerOnly) {
        this.useMasterServerOnly = useMasterServerOnly;
    }

    @Override
    public boolean getUseMasterConnectionOnly() {
        return useMasterServerOnly;
    }

    @Override
    public void onReceive(JSONObject data) {
        Handler handler = weakHandler.get();

        if (handler == null) {
            Sync.removeSyncProvider(Sync.PROVIDER_PING);
            return;
        }

        try {
            if (data.has("status") && data.getString("status").equals("Success")) {
                Sync.removeSyncProvider(Sync.PROVIDER_PING);
                handler.sendEmptyMessage(-1);
                return;
            }

        } catch (JSONException e) {
            //e.printStackTrace();
        }

        handler.sendEmptyMessage(-2);
    }
}
