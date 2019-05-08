package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.network.Receiver;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;

import java.net.InetAddress;

public class SyncBehavior extends NavigationFragment {

    private TextView syncClientPort;
    private TextView syncDiscoveryPort;
    private TextView syncDiscoveryAttempts;
    private TextView syncDashboardInterval;
    private TextView syncMessagesInterval;
    private TextView syncNotificationsInterval;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.sync_behavior, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {

        syncClientPort = view.findViewById(R.id.syncClientPort);
        syncDiscoveryPort = view.findViewById(R.id.syncDiscoveryPort);
        syncDiscoveryAttempts = view.findViewById(R.id.syncDiscoveryAttempts);
        syncDashboardInterval = view.findViewById(R.id.syncDashboardInterval);
        syncMessagesInterval = view.findViewById(R.id.syncMessagesInterval);
        syncNotificationsInterval = view.findViewById(R.id.syncNotificationsInterval);

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        };

        view.findViewById(R.id.syncBehaviorSave).setOnClickListener(c);
        reload();
    }

    @Override
    protected void reload() {
        syncClientPort.setText(
                String.valueOf(DataLoader.getInt("SyncClientPort", 44501))
        );

        syncDiscoveryPort.setText(
                String.valueOf(DataLoader.getInt("SyncDiscoveryPort", 44500))
        );

        syncDiscoveryAttempts.setText(
                String.valueOf(DataLoader.getInt("SyncDiscoveryAttempts", 3))
        );

        syncDashboardInterval.setText(
                String.valueOf(DataLoader.getInt("SyncDashboardInterval", 5000))
        );

        syncMessagesInterval.setText(
                String.valueOf(DataLoader.getInt("SyncMessagesInterval", 500))
        );

        syncNotificationsInterval.setText(
                String.valueOf(DataLoader.getInt("SyncNotificationsInterval", 2000))
        );

    }

    private void save() {
        try {
            int clientPort = Integer.valueOf(syncClientPort.getText().toString());
            int discoveryPort = Integer.valueOf(syncDiscoveryPort.getText().toString());
            int discoveryAttempts = Integer.valueOf(syncDiscoveryAttempts.getText().toString());
            int dashboardInterval = Integer.valueOf(syncDashboardInterval.getText().toString());
            int messagesInterval = Integer.valueOf(syncMessagesInterval.getText().toString());
            int notificationsInterval = Integer.valueOf(syncNotificationsInterval.getText().toString());

            discoveryAttempts = Math.max(discoveryAttempts, 1);
            dashboardInterval = Math.max((dashboardInterval / 500) * 500, 500);
            messagesInterval = Math.max((messagesInterval / 500) * 500, 500);
            notificationsInterval = Math.max((notificationsInterval / 500) * 500, 500);

            DataLoader.set("SyncClientPort", clientPort);
            DataLoader.set("SyncDiscoveryPort", discoveryPort);
            DataLoader.set("SyncDiscoveryAttempts", discoveryAttempts);
            DataLoader.set("SyncDashboardInterval", dashboardInterval);
            DataLoader.set("SyncMessagesInterval", messagesInterval);
            DataLoader.set("SyncNotificationsInterval", notificationsInterval);
            DataLoader.save();
            reload();

            Receiver.stop();
            Receiver.start();

            NotificationsLoader.makeToast("Applied", true);

        } catch (NumberFormatException e) {
            NotificationsLoader.makeToast("Unexpected error", true);
            //e.printStackTrace();
        }
    }
}
