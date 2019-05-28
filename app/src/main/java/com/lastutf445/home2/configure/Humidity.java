package com.lastutf445.home2.configure;

import android.support.annotation.NonNull;

import com.lastutf445.home2.util.Configure;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class Humidity extends Configure {

    public static boolean validateState(@NonNull JSONObject ops, @NonNull JSONObject values) {
        try {
            Iterator<String> it1 = ops.keys();
            while (it1.hasNext()) {
                String key = it1.next();
                Object val = ops.get(key);
                switch (key) {
                    case "channel":
                        if (!(val instanceof String)) {
                            return false;
                        }
                        break;
                    case "delay":
                        if (!(val instanceof Integer)) {
                            return false;
                        }
                        break;
                    default:
                        return false;
                }
            }
            Iterator<String> it2 = values.keys();
            while (it2.hasNext()) {
                String key = it2.next();
                try {
                    Object val = values.get(key);
                    switch (key) {
                        case "nothing":
                        case "humidity":
                            if (!(val instanceof Integer)) {
                                return false;
                            }
                            break;
                        default:
                            return false;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return true;

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }


}
