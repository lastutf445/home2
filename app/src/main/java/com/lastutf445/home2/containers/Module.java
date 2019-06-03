package com.lastutf445.home2.containers;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;
import com.lastutf445.home2.util.JSONPayload;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;

public class Module extends JSONPayload {

    private int serial;
    private boolean syncing;
    @Nullable
    private String type, title;
    private JSONObject values;
    private InetAddress ip;
    private int port;

    public Module(int serial, String type, String ip, int port, String title, @NonNull JSONObject ops, @NonNull JSONObject values, boolean syncing) throws IOException {
        this.serial = serial;
        this.type = type;
        this.ip = InetAddress.getByName(ip);
        this.port = port;
        this.title = title;
        this.syncing = syncing;
        this.ops = ops;
        this.values = values;
    }

    public int getSerial() {
        return serial;
    }

    @NonNull
    public String getType() {
        return type == null ? DataLoader.getAppResources().getString(R.string.unknownType) : type;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @NonNull
    public String getStyledType() {
        Resources res = DataLoader.getAppResources();
        String unknownType = res.getString(R.string.unknownType);

        if (type == null) return unknownType;

        switch (type) {
            case "temperature":
                return res.getString(R.string.modulesTypeTemperature);
            case "humidity":
                return res.getString(R.string.modulesTypeHumidity);
            case "lightrgb":
                return res.getString(R.string.modulesTypeLightRGB);
            case "socket":
                return res.getString(R.string.modulesTypeSocket);
            default:
                return unknownType;
        }
    }

    @NonNull
    public String getTitle() {
        return title == null ? getDefaultTitle(type) : title;
    }

    public static String getDefaultTitle(String type) {
        Resources res = DataLoader.getAppResources();

        switch (type) {
            case "temperature":
                return res.getString(R.string.defaultTitleTemperature);
            case "humidity":
                return res.getString(R.string.defaultTitleHumidity);
            case "lightrgb":
                return res.getString(R.string.defaultTitleLightRGB);
            case "socket":
                return res.getString(R.string.defaultTitleSocket);
            default:
                return res.getString(R.string.defaultTitleUnknownModule);
        }
    }

    public int getIcon() {
        return getIcon(type);
    }

    public static int getIcon(String type) {
        switch (type) {
            case "temperature":
                return R.drawable.thermometer;
            case "humidity":
                return R.drawable.water_outline;
            case "lightrgb":
                return R.drawable.light_bulb;
            case "socket":
                return R.drawable.power_plug;
            default:
                return R.drawable.warning;
        }

    }

    public JSONObject getVals() {
        return values;
    }

    public boolean getSyncing() {
        return syncing;
    }

    public void setSyncing(boolean syncing) {
        this.syncing = syncing;
        //ModulesLoader.onModuleSyncingChanged(this, syncing);
        save();
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
        WidgetsLoader.onModuleTitleUpdated(this);
        save();
    }

    public void mergeStates(@Nullable String type, @Nullable JSONObject ops, @Nullable JSONObject values) {
        if (type == null || ops == null || values == null) {
            // TODO: notify about it
            Log.d("LOGTAG", "validation error on serial: " + serial);
            return;
        }

        //Log.d("LOGTAG", "validating: " + type + " " + ops.toString() + " " + values.toString());

        if (!ModulesLoader.validateState(this, type, ops, values)) {
            // TODO: notify about it
            Log.d("LOGTAG", "validation error on serial: " + serial);
            return;
        }

        try {
            Iterator<String> it1 = ops.keys();
            while (it1.hasNext()) {
                String key = it1.next();
                this.ops.put(key, ops.get(key));
            }
            Iterator<String> it2 = values.keys();
            while (it2.hasNext()) {
                String key = it2.next();
                this.values.put(key, values.get(key));
            }

        } catch (JSONException e) {
            e.printStackTrace();
            // todo: transaction reverse?
        }

        WidgetsLoader.onModuleStateUpdated(this);
        set("lastUpdated", System.currentTimeMillis());
        save();
    }

    public void wipe() {
        ops = new JSONObject();
        values = new JSONObject();
        //WidgetsLoader.onModuleStateUpdated(this);
        //set("lastUpdated", System.currentTimeMillis());
        //save();
    }

    public void save() {
        AsyncSaveTask task = new AsyncSaveTask(this);
        task.execute();
    }

    private static class AsyncSaveTask extends AsyncTask<Void, Void, Void> {
        private Module module;

        public AsyncSaveTask(@NonNull Module module) {
            this.module = module;
        }

        @Nullable
        @Override
        protected Void doInBackground(Void... voids) {
            ModulesLoader.saveState(module);
            return null;
        }
    }
}
