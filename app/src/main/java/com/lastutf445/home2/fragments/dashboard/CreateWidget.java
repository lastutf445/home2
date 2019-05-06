package com.lastutf445.home2.fragments.dashboard;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.containers.Widget;
import com.lastutf445.home2.fragments.menu.Modules;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;
import com.lastutf445.home2.util.NavigationFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class CreateWidget extends NavigationFragment {

    private LayoutInflater inflater;
    private LinearLayout content;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.widgets_create, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        inflater = getLayoutInflater();
        content = view.findViewById(R.id.createWidgetInclude);

        content.addView(getTitleView());
        content.addView(getModuleView());

        content.getChildAt(0).setVisibility(View.GONE);
        content.getChildAt(1).setVisibility(View.GONE);

        ((RadioGroup) view.findViewById(R.id.createWidgetRadios)).setOnCheckedChangeListener(
            new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        case R.id.createWidgetRadioTitle:
                            content.getChildAt(1).setVisibility(View.GONE);
                            content.getChildAt(0).setVisibility(View.VISIBLE);
                            break;
                        case R.id.createWidgetRadioModule:
                            content.getChildAt(0).setVisibility(View.GONE);
                            content.getChildAt(1).setVisibility(View.VISIBLE);
                            break;
                    }
                }
            }
        );
    }

    private View getTitleView() {
        View v = inflater.inflate(R.layout.widgets_title, content, false);

        v.findViewById(R.id.createTitleApply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = ((EditText) view.findViewById(R.id.createTitleTitle)).getText().toString();
                if (s.length() != 0) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("title", s);

                        Widget widget = WidgetsLoader.createUtilWidget("title", json);

                        if (!WidgetsLoader.addWidget(widget)) {
                            NotificationsLoader.makeToast("Unexpected error", true);
                        } else {
                            NotificationsLoader.makeToast("Created", true);
                            getActivity().onBackPressed();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        NotificationsLoader.makeToast("Unexpected error", true);
                    }
                } else {
                    NotificationsLoader.makeToast("Title can't be empty", true);
                }
            }
        });

        return v;
    }

    private View getModuleView() {
        View v = inflater.inflate(R.layout.widgets_module, content, false);

        v.findViewById(R.id.craeteModuleList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Modules modules = new Modules();
                modules.setModules(WidgetsLoader.getFree(), true);
                FragmentsLoader.addChild(modules, CreateWidget.this);
            }
        });

        v.findViewById(R.id.createModuleApply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = ((TextView) view.findViewById(R.id.createModuleSerial)).getText().toString();
                if (!s.equals(DataLoader.getAppResources().getString(R.string.undefined))) {
                    try {
                        Module module = ModulesLoader.getModule(Integer.valueOf(s));

                        if (module == null) {
                            NotificationsLoader.makeToast("Module not found", true);
                            return;
                        }

                        Widget widget = WidgetsLoader.createWidget(module);

                        if (widget == null || !WidgetsLoader.addWidget(widget)) {
                            NotificationsLoader.makeToast("Unexpected error", true);

                        } else {
                            NotificationsLoader.makeToast("Created", true);
                            getActivity().onBackPressed();
                        }

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        NotificationsLoader.makeToast("Unexpected error", true);
                    }

                } else {
                    NotificationsLoader.makeToast("Please, choose a module", true);
                }
            }
        });

        return v;
    }

    @Override
    public void onResult(Bundle data) {
        if (data == null) return;

        Module module = ModulesLoader.getModule(data.getInt("updated", -1));

        String serial = DataLoader.getAppResources().getString(R.string.undefined);
        String title = DataLoader.getAppResources().getString(R.string.unknownModule);

        if (module != null) {
            serial = String.valueOf(module.getSerial());
            title = module.getTitle();
        }

        ((TextView) view.findViewById(R.id.createModuleTitle)).setText(title);
        ((TextView) view.findViewById(R.id.createModuleSerial)).setText(serial);
    }
}