package com.lastutf445.home2.containers;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NodesLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;
import com.lastutf445.home2.util.JSONPayload;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

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
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
        WidgetsLoader.onModuleTitleUpdated(this);
    }

    public void updateState(JSONObject ops) {
        this.ops = ops;
        WidgetsLoader.onModuleStateUpdated(this);
    }
}
