package com.lastutf445.home2.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.network.Sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class ModuleEditRequest extends SyncProvider {

    private WeakReference<Handler> weakHandler;
    @NonNull
    private JSONObject ops = new JSONObject();
    private boolean tainted = false;
    private String type;
    private int serial;

    public ModuleEditRequest(@NonNull Module module, @NonNull Handler handler) throws JSONException {
        super(Sync.PROVIDER_MODULE_EDIT_REQUEST, "moduleEditRequest", new JSONObject(), module.getIp(), module.getPort(), false);
        weakHandler = new WeakReference<>(handler);
        serial = module.getSerial();
        type = module.getType();
    }

    public void setOps(@NonNull JSONObject ops) {
        this.ops = ops;
        tainted = true;
    }

    @Override
    public void onReceive(@NonNull JSONObject data) {
        Handler handler = weakHandler.get();

        try {
            int status = data.getInt("status");

            if (status == Sync.OK) {
                if (handler != null) {
                    Message msg = handler.obtainMessage(1);
                    Bundle msgData = new Bundle();

                    msgData.putString("ops", data.getString("ops"));
                    msg.setData(msgData);

                    handler.sendMessage(msg);
                }
                return;
            }

            if (handler != null) {
                Message msg = handler.obtainMessage(0);
                Bundle msgData = new Bundle();

                msgData.putInt("status", data.getInt("status"));
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
                data.put("type", type);
                data.put("ops", ops);
                query.put("data", data);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return query;
    }
}
