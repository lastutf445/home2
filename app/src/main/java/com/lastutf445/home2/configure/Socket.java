package com.lastutf445.home2.configure;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.util.Configure;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class Socket extends Configure {

    private boolean rendered = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.configure_socket, container, false);
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
                    case R.id.socketON:
                        switchState();
                        break;
                    case R.id.socketDisableAfterInit:
                        disableAfterInit();
                        break;
                    case R.id.socketDisableWhenIdle:
                        disableWhenIdle();
                        break;
                    case R.id.socketActiveStateTimeoutSave:
                        changeTimeout();
                        break;
                }
            }
        };

        View.OnFocusChangeListener f = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //Log.d("LOGTAG", "rendered = true");
                    rendered = true;
                }
            }
        };

        view.findViewById(R.id.socketON).setOnClickListener(c);
        view.findViewById(R.id.socketDisableAfterInit).setOnClickListener(c);
        view.findViewById(R.id.socketDisableWhenIdle).setOnClickListener(c);
        view.findViewById(R.id.socketActiveStateTimeoutSave).setOnClickListener(c);
        view.findViewById(R.id.socketActiveStateTimeout).setOnFocusChangeListener(f);

        setRender(new Configure.Render() {
            @Override
            public void reload(@NonNull View view, @NonNull Module module) {
                ((TextView) view.findViewById(R.id.socketTitle)).setText(
                        module.getTitle()
                );

                boolean state = module.getBoolean("enabled", false);
                ((Switch) view.findViewById(R.id.socketONSwitch)).setChecked(state);

                ((ImageView) view.findViewById(R.id.socketIcon)).setImageTintList(
                        ColorStateList.valueOf(Color.parseColor(state ? "#00695C" : "#333333"))
                );

                if (!module.has("disableAfterInit")) {
                    view.findViewById(R.id.socketDisableAfterInit).setVisibility(View.GONE);
                } else {
                    ((Switch) view.findViewById(R.id.socketDisableAfterInitSwitch)).setChecked(
                            module.getBoolean("disableAfterInit", false)
                    );
                }

                if (!module.has("disableWhenIdle")) {
                    view.findViewById(R.id.socketAdvanced).setVisibility(View.GONE);
                    view.findViewById(R.id.socketDisableWhenIdle).setVisibility(View.GONE);
                    view.findViewById(R.id.socketActiveStateTimeoutWrapper).setVisibility(View.GONE);
                    view.findViewById(R.id.socketActiveStateTimeoutSave).setVisibility(View.GONE);

                } else {
                    TextView timeout = view.findViewById(R.id.socketActiveStateTimeout);
                    Button save = view.findViewById(R.id.socketActiveStateTimeoutSave);

                    view.findViewById(R.id.socketAdvanced).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.socketDisableWhenIdle).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.socketActiveStateTimeoutWrapper).setVisibility(View.VISIBLE);
                    save.setVisibility(View.VISIBLE);

                    ((Switch) view.findViewById(R.id.socketDisableWhenIdleSwitch)).setChecked(
                            module.getBoolean("disableWhenIdle", false)
                    );

                    save.setEnabled(
                            module.getBoolean("disableWhenIdle", false)
                    );

                    save.setTextColor(
                             Color.parseColor(save.isEnabled() ? "#00796B" : "#aaaaaa")
                    );

                    timeout.setEnabled(
                            module.getBoolean("disableWhenIdle", false)
                    );

                    if (!save.isEnabled()) {
                        rendered = false;
                    }

                    //Log.d("LOGTAG", "rendered == " + rendered);

                    if (!rendered) {
                        timeout.clearFocus();
                        timeout.setText(
                                String.valueOf(module.getInt("activeStateTimeout", 0))
                        );
                    }
                }
            }
        });
    }

    private void switchState() {
        boolean state = module.getBoolean("enabled", false);

        try {
            JSONObject ops = new JSONObject();
            ops.put("enabled", !state);
            makeEditRequest(ops);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void disableAfterInit() {
        boolean state = module.getBoolean("disableAfterInit", false);

        try {
            JSONObject ops = new JSONObject();
            ops.put("disableAfterInit", !state);
            makeEditRequest(ops);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void disableWhenIdle() {
        boolean state = module.getBoolean("disableWhenIdle", false);

        try {
            JSONObject ops = new JSONObject();
            ops.put("disableWhenIdle", !state);
            makeEditRequest(ops);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void changeTimeout() {
        try {
            int timeout = Integer.valueOf(
                    ((TextView) view.findViewById(R.id.socketActiveStateTimeout)).getText().toString()
            );

            JSONObject ops = new JSONObject();
            ops.put("activeStateTimeout", timeout);
            makeEditRequest(ops);
            return;

        } catch (JSONException e) {
            e.printStackTrace();

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        NotificationsLoader.makeToast("Unexpected error", true);
        reload();
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
                    case "disableAfterInit":
                    case "disableWhenIdle":
                    case "enabled":
                        if (!(val instanceof Boolean)) {
                            return false;
                        }
                        break;
                    case "activeStateTimeout":
                        if (!(val instanceof Integer)) {
                            return false;
                        }
                        break;
                    default:
                        return false;
                }
            }

            if (ops.has("activeStateTimeout") && ops.getInt("activeStateTimeout") <= 0) {
                return false;
            }

            Iterator<String> it2 = values.keys();
            while (it2.hasNext()) {
                String key = it2.next();
                try {
                    Object val = values.get(key);
                    switch (key) {
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
