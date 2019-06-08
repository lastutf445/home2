package com.lastutf445.home2.configure;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;

public class Temperature extends Configure {

    private NumberFormat formatter;
    private boolean rendered = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.configure_temperature, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(view, module);
        formatter = new DecimalFormat("#0.0 °C");

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.tempRefresh:
                        refreshState();
                        break;
                    case R.id.tempRefreshPeriodSave:
                        saveRefresh();
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

        view.findViewById(R.id.tempRefresh).setOnClickListener(c);
        view.findViewById(R.id.tempRefreshPeriodSave).setOnClickListener(c);
        view.findViewById(R.id.tempRefreshPeriod).setOnFocusChangeListener(f);

        setRender(new Configure.Render() {
            @Override
            public void reload(@NonNull View view, @NonNull Module module) {
                ((TextView) view.findViewById(R.id.tempTitle)).setText(
                        module.getTitle()
                );

                try {
                    ((TextView) view.findViewById(R.id.tempTemp)).setText(
                            formatter.format(module.getVals().getDouble("temp"))
                    );

                } catch (JSONException e) {
                    ((TextView) view.findViewById(R.id.tempTemp)).setText(
                            R.string.notAvailableShort
                    );

                    //e.printStackTrace();
                }

                Switch refresh = view.findViewById(R.id.tempRefreshSwitch);
                Button save = view.findViewById(R.id.tempRefreshPeriodSave);
                EditText refreshPeriod = view.findViewById(R.id.tempRefreshPeriod);

                refresh.setChecked(module.getBoolean("refresh", false));

                if (module.has("refreshPeriod")) {
                    view.findViewById(R.id.tempModulesSettings).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.tempRefreshPeriodWrapper).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.tempRefreshPeriodSave).setVisibility(View.VISIBLE);
                    save.setEnabled(refresh.isChecked());

                    if (!save.isEnabled()) {
                        rendered = false;
                    }

                    //Log.d("LOGTAG", "rendered == " + rendered);

                    if (!rendered) {
                        refreshPeriod.clearFocus();
                        refreshPeriod.setText(
                                String.valueOf(module.getInt("refreshPeriod", 0))
                        );
                    }

                    refreshPeriod.setEnabled(save.isEnabled());

                    save.setTextColor(
                            Color.parseColor(save.isEnabled() ? "#00796B" : "#aaaaaa")
                    );

                } else {
                    view.findViewById(R.id.tempModulesSettings).setVisibility(View.GONE);
                    view.findViewById(R.id.tempRefreshPeriodWrapper).setVisibility(View.GONE);
                    view.findViewById(R.id.tempRefreshPeriodSave).setVisibility(View.GONE);
                    rendered = false;
                }
            }
        });
    }

    private void refreshState() {
        boolean state = module.getBoolean("refresh", false);

        try {
            JSONObject ops = new JSONObject();
            ops.put("refresh", !state);
            makeEditRequest(ops);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveRefresh() {
        try {
            float timeout = Float.valueOf(
                    ((EditText) view.findViewById(R.id.tempRefreshPeriod)).getText().toString()
            );

            JSONObject ops = new JSONObject();
            ops.put("refreshPeriod", timeout);
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
                    case "refreshPeriod":
                        if (!(val instanceof Integer)) {
                            return false;
                        }
                        break;
                    case "refresh":
                        if (!(val instanceof Boolean)) {
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
                        case "temp":
                            if (!(val instanceof Double)) {
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
