package com.lastutf445.home2.containers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.util.JSONPayload;

import org.json.JSONObject;

public class Widget extends JSONPayload {

    private int id, serial;
    @Nullable
    private String type;
    private View view;

    public Widget(int id, int serial, @Nullable String type, @Nullable JSONObject ops) {
        this.serial = serial;
        this.type = type;
        this.ops = ops;
        this.id = id;
    }

    public void setView(@NonNull View view) {
        this.view = view;
    }

    @Nullable
    public View getView() {
        return view;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSerial() {
        return serial;
    }

    @NonNull
    public String getType() {
        return type == null ? DataLoader.getAppResources().getString(R.string.unknownType) : type;
    }

    public int getIcon() {
        switch (type) {
            case "title":
                return R.drawable.title;
            default:
                return Module.getIcon(type);
        }
    }
}
