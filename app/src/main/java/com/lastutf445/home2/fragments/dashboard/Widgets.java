package com.lastutf445.home2.fragments.dashboard;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.WidgetsAdapter;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;
import com.lastutf445.home2.util.NavigationFragment;

public class Widgets extends NavigationFragment {

    @Nullable
    private WidgetsAdapter adapter;
    private RecyclerView content;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.widgets, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        content = view.findViewById(R.id.widgetsContent);
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()));

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.widgetsSave:
                        if (WidgetsLoader.save()) {
                            NotificationsLoader.makeToast("Success", true);

                        } else {
                            NotificationsLoader.makeToast("Failed to save, restart the app", true);
                        }
                        break;
                    case R.id.widgetsRestore:
                        NotificationsLoader.makeToast("Restored", true);
                        WidgetsLoader.reload();
                        reload();
                        break;
                }
            }
        };

        view.findViewById(R.id.widgetsSave).setOnClickListener(c);
        view.findViewById(R.id.widgetsRestore).setOnClickListener(c);

        adapter = new WidgetsAdapter(getLayoutInflater(), content, getActivity());
        content.setAdapter(adapter);
        adapter.initCallback();
        reload();
    }

    @Override
    protected void reload() {
        adapter.setData(WidgetsLoader.getWidgets().clone());
    }
}
