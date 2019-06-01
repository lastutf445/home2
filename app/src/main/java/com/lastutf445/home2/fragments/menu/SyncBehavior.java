package com.lastutf445.home2.fragments.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class SyncBehavior extends NavigationFragment {

    private TextView syncClientPort;
    private TextView syncDiscoveryPort;
    private TextView syncDiscoveryTimeout;
    private TextView syncPingAttempts;
    private TextView syncPingInterval;

    private Updater updater;

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
        syncDiscoveryTimeout = view.findViewById(R.id.syncDiscoveryTimeout);
        syncPingAttempts = view.findViewById(R.id.syncPingAttempts);
        syncPingInterval = view.findViewById(R.id.syncPingInterval);

        updater = new Updater(view);
        UserLoader.setSettingsHandler(updater);

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
        updater.sendEmptyMessage(-1);
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
                syncDiscoveryTimeout.setText(String.valueOf(3));
                syncPingAttempts.setText(String.valueOf(3));
                syncPingInterval.setText(String.valueOf(1000));
            }
        });

        builder.create().show();
    }

    private void save() {
        try {
            int clientPort = Integer.valueOf(syncClientPort.getText().toString());
            int discoveryPort = Integer.valueOf(syncDiscoveryPort.getText().toString());
            int discoveryTimeout = Integer.valueOf(syncDiscoveryTimeout.getText().toString());
            int pingAttempts = Integer.valueOf(syncPingAttempts.getText().toString());
            int pingInterval = Integer.valueOf(syncPingInterval.getText().toString());

            discoveryTimeout = Math.max(discoveryTimeout, 1);
            pingAttempts = Math.max(pingAttempts, 1);
            pingInterval = Math.max((pingInterval / 500) * 500, 500);

            if (clientPort != DataLoader.getInt("SyncClientPort", 44501)) {
                DataLoader.set("SyncClientPort", clientPort);
            }

            if (discoveryPort != DataLoader.getInt("SyncDiscoveryPort", 44500)) {
                DataLoader.set("SyncDiscoveryPort", discoveryPort);
            }

            if (discoveryTimeout != DataLoader.getInt("SyncDiscoveryTimeout", 3)) {
                DataLoader.set("SyncDiscoveryTimeout", discoveryTimeout);
            }

            if (pingAttempts != DataLoader.getInt("SyncPingAttempts", 3)) {
                DataLoader.set("SyncPingAttempts", pingAttempts);
            }

            if (pingInterval != DataLoader.getInt("SyncPingInterval", 1000)) {
                DataLoader.set("SyncPingInterval", pingInterval);
            }

            DataLoader.save();
            reload();

            Sync.restart();
            NotificationsLoader.makeToast("Applied", true);
        } catch (NumberFormatException e) {
            NotificationsLoader.makeToast("Unexpected error", true);
            //e.printStackTrace();
        }
    }

    private static class Updater extends Handler {
        private WeakReference<View> weakView;

        public Updater(View view) {
            weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == -1) reload();
        }

        private void reload() {
            View view = weakView.get();

            if (view != null) {
                ((TextView) view.findViewById(R.id.syncClientPort)).setText(
                        String.valueOf(DataLoader.getInt("SyncClientPort", 44501))
                );

                ((TextView) view.findViewById(R.id.syncDiscoveryPort)).setText(
                        String.valueOf(DataLoader.getInt("SyncDiscoveryPort", 44500))
                );

                ((TextView) view.findViewById(R.id.syncDiscoveryTimeout)).setText(
                        String.valueOf(DataLoader.getInt("SyncDiscoveryTimeout", 3))
                );

                ((TextView) view.findViewById(R.id.syncPingAttempts)).setText(
                        String.valueOf(DataLoader.getInt("SyncPingAttempts", 3))
                );

                ((TextView) view.findViewById(R.id.syncPingInterval)).setText(
                        String.valueOf(DataLoader.getInt("SyncPingInterval", 1000))
                );
            }
        }
    }
}
