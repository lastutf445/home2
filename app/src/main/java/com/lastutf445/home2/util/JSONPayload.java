package com.lastutf445.home2.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONPayload {

    @Nullable
    protected JSONObject ops = new JSONObject();

    @NonNull
    public JSONObject getOps() {
        return ops;
    }

    public <T> void set(@NonNull String key, T value) {
        try {
            ops.put(key, value);

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    public boolean has(String key) {
        return ops.has(key);
    }

    @Nullable
    public Object get(String key) {
        try {
            return ops.get(key);

        } catch (JSONException e) {
            //e.printStackTrace();
            return null;
        }
    }

    public boolean getBoolean(String key, boolean std) {
        Object res = get(key);
        return res != null ? (boolean) res : std;
    }

    public int getInt(String key, int std) {
        Object res = get(key);
        return res != null ? (int) res : std;
    }

    public long getLong(String key, long std) {
        Object res = get(key);
        return res != null ? (long) res : std;
    }

    @NonNull
    public String getString(String key, @NonNull String std) {
        Object res = get(key);
        return res != null ? (String) res : std;
    }
}
