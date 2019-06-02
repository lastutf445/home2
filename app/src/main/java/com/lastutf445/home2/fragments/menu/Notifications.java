package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Notifications extends NavigationFragment {

    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_notifications, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(view);

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.notificationsEnabled:
                        DataLoader.set("NotificationsEnabled",
                                !DataLoader.getBoolean("NotificationsEnabled", false)
                        );
                        break;
                    case R.id.suppressModulesStateSync:
                        DataLoader.set("SuppressModulesStateSync",
                                !DataLoader.getBoolean("SuppressModulesStateSync", false)
                        );
                        break;
                    case R.id.suppressModulesStateSyncFailed:
                        DataLoader.set("SuppressModulesStateSyncFailed",
                                !DataLoader.getBoolean("SuppressModulesStateSyncFailed", false)
                        );
                        break;
                    case R.id.suppressUserDataSync:
                        DataLoader.set("SuppressUserDataSync",
                                !DataLoader.getBoolean("SuppressUserDataSync", false)
                        );
                        break;
                    case R.id.suppressUserDataSyncFailed:
                        DataLoader.set("SuppressUserDataSyncFailed",
                                !DataLoader.getBoolean("SuppressUserDataSyncFailed", false)
                        );
                        break;
                }

                updater.sendEmptyMessage(-1);
            }
        };

        view.findViewById(R.id.notificationsEnabled).setOnClickListener(c);
        view.findViewById(R.id.suppressModulesStateSync).setOnClickListener(c);
        view.findViewById(R.id.suppressModulesStateSyncFailed).setOnClickListener(c);
        view.findViewById(R.id.suppressUserDataSync).setOnClickListener(c);
        view.findViewById(R.id.suppressUserDataSyncFailed).setOnClickListener(c);

        updater.sendEmptyMessage(-1);
    }

    @Override
    public void onResult(Bundle data) {
        updater.sendEmptyMessage(-1);
        UserLoader.setSettingsHandler(updater);
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
                ((Switch) view.findViewById(R.id.notificationsEnabledSwitch)).setChecked(
                        DataLoader.getBoolean("NotificationsEnabled", false)
                );

                ((CheckBox) view.findViewById(R.id.suppressModulesStateSyncCheckBox)).setChecked(
                        DataLoader.getBoolean("SuppressModulesStateSync", false)
                );

                ((CheckBox) view.findViewById(R.id.suppressModulesStateSyncFailedCheckBox)).setChecked(
                        DataLoader.getBoolean("SuppressModulesStateSyncFailed", false)
                );

                ((CheckBox) view.findViewById(R.id.suppressUserDataSyncCheckBox)).setChecked(
                        DataLoader.getBoolean("SuppressUserDataSync", false)
                );

                ((CheckBox) view.findViewById(R.id.suppressUserDataSyncFailedCheckBox)).setChecked(
                        DataLoader.getBoolean("SuppressUserDataSyncFailed", false)
                );
            }
        }
    }
}
