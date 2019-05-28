package com.lastutf445.home2.configure;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.util.Configure;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class Socket extends Configure {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.special_socket, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(view, module);

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.specialSocketIsOn:
                        switchState();
                        break;
                }
            }
        };

        view.findViewById(R.id.specialSocketIsOn).setOnClickListener(c);

        setRender(new Configure.Render() {
            @Override
            public void reload(@NonNull View view, @NonNull Module module) {
                ((TextView) view.findViewById(R.id.specialTitle)).setText(
                        module.getTitle()
                );

                ((Switch) view.findViewById(R.id.specialSocketIsOnCheckBox)).setChecked(
                        module.getBoolean("value", false)
                );
            }
        });

        reload();
    }

    private void switchState() {
        boolean state = module.getBoolean("value", false);

        try {
            JSONObject ops = new JSONObject();
            ops.put("value", !state);
            makeEditRequest(ops);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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
                    case "enabled":
                        if (!(val instanceof Boolean)) {
                            return false;
                        }
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
                        case "current":
                            if (!(val instanceof Boolean)) {
                                return false;
                            }
                            break;
                        case "nothing":
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
