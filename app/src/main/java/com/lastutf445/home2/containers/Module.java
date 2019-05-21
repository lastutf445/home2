package com.lastutf445.home2.containers;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NodesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;
import com.lastutf445.home2.util.JSONPayload;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;

public class Module extends JSONPayload {

    private int serial, node;
    private boolean syncing;
    private String type, title;

    public Module(int serial, String type, int node, String title, String options, int syncing) throws JSONException {
        this.serial = serial;
        this.type = type;
        this.node = node;
        this.title = title;
        this.syncing = syncing > 0;
        this.ops = new JSONObject(options);
    }

    public int getSerial() {
        return serial;
    }

    @NonNull
    public String getType() {
        return type == null ? DataLoader.getAppResources().getString(R.string.unknownType) : type;
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

    public int getNode() {
        return node;
    }

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

    public boolean getSyncing() {
        return syncing;
    }

    public void setSyncing(boolean syncing) {
        this.syncing = syncing;
        NodesLoader.onModuleSyncingChanged(this);
        save();
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
        WidgetsLoader.onModuleTitleUpdated(this);
        save();
    }

    public void mergeStates(String type, JSONObject ops) {
        if(type == null || ops == null) return;
        boolean wiped = false;

        if (this.ops.has("wiped")) {
            wiped = true;
        }

        if (!this.type.equals(type)) {
            Log.d("LOGTAG", "validation error on serial: " + serial);
            return;
        }

        Iterator<String> it = ops.keys();
        boolean pass = true;

        try {
            while (it.hasNext()) {
               String key = it.next();
                if (!this.ops.has(key) || !validateField(this.ops.get(key), ops.get(key))) {
                    pass = false;
                    break;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            pass = false;
        }

        if (!pass && !wiped) {
            Log.d("LOGTAG", "validation error on serial: " + serial);
            return;
        }

        it = ops.keys();

        while (it.hasNext()) {
            try {
                String key = it.next();
                this.ops.put(key, ops.get(key));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        this.ops.remove("wiped");
        WidgetsLoader.onModuleStateUpdated(this);
        set("lastUpdated", System.currentTimeMillis());
        save();
    }

    public void wipe() {
        this.ops = new JSONObject();

        try {
            ops.put("wiped", true);

        } catch (JSONException e) {
            Log.d("LOGTAG", "wtf?");
        }

        WidgetsLoader.onModuleStateUpdated(this);
        save();
    }

    private boolean validateField(Object a, Object b) {
        if (a == null) return true;
        if (b == null) return false;
        return a.getClass().getName().equals(b.getClass().getName());
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

        @Override
        protected Void doInBackground(Void... voids) {
            ModulesLoader.applyModule(module);
            return null;
        }
    }
}
