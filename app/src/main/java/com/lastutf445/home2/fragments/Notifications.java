package com.lastutf445.home2.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.NotificationsAdapter;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.util.NavigationFragment;

public class Notifications extends NavigationFragment {

    private NotificationsAdapter adapter;
    private RecyclerView content;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_notifications, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        content = view.findViewById(R.id.notificationsContent);
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()));

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    // todo: button?
                }
            }
        };

        //view.findViewById(R.id.notificationsClear).setOnClickListener(c);

        adapter = new NotificationsAdapter(getLayoutInflater(), content, getActivity());
        content.setAdapter(adapter);
        adapter.initCallback();
        reload();
    }

    @Override
    protected void reload() {
        adapter.setData(NotificationsLoader.getNotifications());

        view.findViewById(R.id.notificationsNoContent).setVisibility(
                adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE
        );
    }
}
