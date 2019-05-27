package com.lastutf445.home2.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.network.Sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

public class ModuleEditRequest extends SyncProvider {

    private WeakReference<Handler> weakHandler;
    private JSONObject ops = new JSONObject();
    private boolean tainted = false;
    private int serial = -1;

    public ModuleEditRequest(@NonNull Module module, @NonNull Handler handler) throws JSONException {
        super(Sync.PROVIDER_MODULE_EDIT_REQUEST, "edit", new JSONObject(), module.getIp(), module.getPort());
        weakHandler = new WeakReference<>(handler);
    }

    public void setSerial(int serial) {
        this.serial = serial;
        tainted = true;
    }

    public void setOps(@NonNull JSONObject ops) {
        this.ops = ops;
        tainted = true;
    }

    @Override
    public void onReceive(JSONObject data) {
        Handler handler = weakHandler.get();

        try {
            boolean status = data.getBoolean("success");

            if (status && handler != null) {
                Message msg = handler.obtainMessage(1);
                Bundle msgData = new Bundle();

                msgData.putString("ops", data.getString("ops"));
                msg.setData(msgData);

                handler.sendMessage(msg);
                return;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (handler != null) {
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    public JSONObject getQuery() {
        if (tainted) {
            try {
                JSONObject data = new JSONObject();
                data.put("serial", serial);
                data.put("ops", ops);
                query.put("data", data);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return query;
    }
}
