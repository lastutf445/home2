package com.lastutf445.home2.fragments.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
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
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;

public class SyncBehavior extends NavigationFragment {

    private TextView syncClientPort;
    private TextView syncDiscoveryPort;
    private TextView syncDiscoveryAttempts;
    private TextView syncPingAttempts;
    private TextView syncPingInterval;
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
        syncPingAttempts = view.findViewById(R.id.syncPingAttempts);
        syncPingInterval = view.findViewById(R.id.syncPingInterval);
        syncDashboardInterval = view.findViewById(R.id.syncDashboardInterval);
        syncMessagesInterval = view.findViewById(R.id.syncMessagesInterval);
        syncNotificationsInterval = view.findViewById(R.id.syncNotificationsInterval);

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.syncBehaviorDefaults:
                        loadDefaults();
                        break;
                    case R.id.syncBehaviorSave:
                        save();
                        break;
                }

            }
        };

        view.findViewById(R.id.syncBehaviorDefaults).setOnClickListener(c);
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

        syncPingAttempts.setText(
                String.valueOf(DataLoader.getInt("SyncPingAttempts", 3))
        );

        syncPingInterval.setText(
                String.valueOf(DataLoader.getInt("SyncPingInterval", 1000))
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

    private void loadDefaults() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        builder.setTitle(res.getString(R.string.loadDefaults));
        builder.setMessage(res.getString(R.string.loadDefaultsMessage));

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.load, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                syncClientPort.setText(String.valueOf(44501));
                syncDiscoveryPort.setText(String.valueOf(44500));
                syncDiscoveryAttempts.setText(String.valueOf(3));
                syncPingAttempts.setText(String.valueOf(3));
                syncPingInterval.setText(String.valueOf(1000));
                syncDashboardInterval.setText(String.valueOf(5000));
                syncMessagesInterval.setText(String.valueOf(500));
                syncNotificationsInterval.setText(String.valueOf(2000));
            }
        });

        builder.create().show();
    }

    private void save() {
        try {
            int clientPort = Integer.valueOf(syncClientPort.getText().toString());
            int discoveryPort = Integer.valueOf(syncDiscoveryPort.getText().toString());
            int discoveryAttempts = Integer.valueOf(syncDiscoveryAttempts.getText().toString());
            int pingAttempts = Integer.valueOf(syncPingAttempts.getText().toString());
            int pingInterval = Integer.valueOf(syncPingInterval.getText().toString());
            int dashboardInterval = Integer.valueOf(syncDashboardInterval.getText().toString());
            int messagesInterval = Integer.valueOf(syncMessagesInterval.getText().toString());
            int notificationsInterval = Integer.valueOf(syncNotificationsInterval.getText().toString());

            discoveryAttempts = Math.max(discoveryAttempts, 1);
            pingAttempts = Math.max(pingAttempts, 1);
            pingInterval = Math.max((pingInterval / 500) * 500, 500);
            dashboardInterval = Math.max((dashboardInterval / 500) * 500, 500);
            messagesInterval = Math.max((messagesInterval / 500) * 500, 500);
            notificationsInterval = Math.max((notificationsInterval / 500) * 500, 500);

            DataLoader.set("SyncClientPort", clientPort);
            DataLoader.set("SyncDiscoveryPort", discoveryPort);
            DataLoader.set("SyncDiscoveryAttempts", discoveryAttempts);
            DataLoader.set("SyncPingAttempts", pingAttempts);
            DataLoader.set("SyncPingInterval", pingInterval);
            DataLoader.set("SyncDashboardInterval", dashboardInterval);
            DataLoader.set("SyncMessagesInterval", messagesInterval);
            DataLoader.set("SyncNotificationsInterval", notificationsInterval);
            DataLoader.save();
            reload();

            Sync.restart();

            NotificationsLoader.makeToast("Applied", true);

        } catch (NumberFormatException e) {
            NotificationsLoader.makeToast("Unexpected error", true);
            //e.printStackTrace();
        }
    }
}
