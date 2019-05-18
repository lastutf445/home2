package com.lastutf445.home2.special;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.util.Special;

import org.json.JSONException;
import org.json.JSONObject;

public class SocketSpecial extends Special {

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

        setRender(new Special.Render() {
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
}
