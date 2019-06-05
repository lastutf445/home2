package com.lastutf445.home2.configure;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.fragments.dialog.ColorPicker;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.util.Configure;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class LightRGB extends Configure {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.configure_lightrgb, container, false);
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
                    case R.id.lightRGBON:
                        switchState();
                        break;
                    case R.id.lightRGBColor:
                        pickColor();
                        break;
                }
            }
        };

        view.findViewById(R.id.lightRGBColor).setOnClickListener(c);
        view.findViewById(R.id.lightRGBON).setOnClickListener(c);

        if (!module.has("color")) {
            view.findViewById(R.id.lightRGBColor).setVisibility(View.GONE);
        }

        setRender(new Configure.Render() {
            @Override
            public void reload(@NonNull View view, @NonNull Module module) {
                ((TextView) view.findViewById(R.id.lightRGBTitle)).setText(
                        module.getTitle()
                );

                ((Switch) view.findViewById(R.id.lightRGBONSwitch)).setChecked(
                        module.getBoolean("lit", false)
                );

                try {
                    int color = Color.parseColor("#333333");

                    if (module.getBoolean("lit", false)) {
                        color = Color.parseColor(module.getString("color", "#008577"));
                    }

                    ((ImageView) view.findViewById(R.id.lightRGBIcon)).setImageTintList(
                            ColorStateList.valueOf(color)
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void switchState() {
        boolean state = module.getBoolean("lit", false);

        try {
            JSONObject ops = new JSONObject();
            ops.put("lit", !state);
            makeEditRequest(ops);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void pickColor() {
        ColorPicker picker = new ColorPicker();
        picker.setColor(module.getString("color", "#000000"));

        picker.setOnColorPicked(new ColorPicker.OnColorPicked() {
            @Override
            public void onColorPicked(String color) {
                //Log.d("LOGTAG", "color - " + color);
                setColor(color);
            }
        });

        picker.show(getChildFragmentManager(), "colorPicker");
    }

    private void setColor(String color) {
        try {
            Color.parseColor(color);

        } catch (Exception e) {
            //e.printStackTrace();
            NotificationsLoader.makeToast("Invalid color", true);
            return;
        }

        try {
            JSONObject ops = new JSONObject();
            ops.put("color", color);
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
                    case "color":
                        if (!(val instanceof String)) {
                            return false;
                        }
                        break;
                    case "lit":
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
