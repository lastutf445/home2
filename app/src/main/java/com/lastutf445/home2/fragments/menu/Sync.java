package com.lastutf445.home2.fragments.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.CryptoLoader;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Receiver;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;

import java.lang.ref.WeakReference;

public class Sync extends NavigationFragment {

    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_sync, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(view);
        UserLoader.setSettingsHandler(updater);

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.syncMarkAsHomeNetwork:
                        String bssid = com.lastutf445.home2.network.Sync.getNetworkBSSID();

                        if (com.lastutf445.home2.network.Sync.getNetworkState() == 2) {
                            if(!DataLoader.getString("SyncHomeNetwork", "false").equals(bssid)) {
                                DataLoader.setWithoutSync("SyncHomeNetwork", bssid);
                            }
                            else {
                                DataLoader.setWithoutSync("SyncHomeNetwork", null);
                            }

                            updater.updateNetworkState();
                            Receiver.stop();
                            Receiver.start();

                            DataLoader.save();
                            reload();
                        }
                        break;
                    case R.id.syncBehavior:
                        FragmentsLoader.addChild(new SyncBehavior(), Sync.this);
                        break;

                    case R.id.syncExternalConnection:
                        FragmentsLoader.addChild(new ExternalAddress(), Sync.this);
                        break;

                    case R.id.syncMasterServer:
                        FragmentsLoader.addChild(new MasterServer(), Sync.this);
                        break;

                    case R.id.syncPersistentConnection:
                        Switch switcher = view.findViewById(R.id.syncPersistentConnectionSwitch);
                        DataLoader.set("SyncPersistentConnection", !switcher.isChecked());
                        switcher.setChecked(!switcher.isChecked());
                        break;

                    case R.id.syncClearKey:
                        clearKey();
                        break;
                }
            }
        };

        view.findViewById(R.id.syncBehavior).setOnClickListener(c);
        view.findViewById(R.id.syncMasterServer).setOnClickListener(c);
        view.findViewById(R.id.syncExternalConnection).setOnClickListener(c);
        view.findViewById(R.id.syncMarkAsHomeNetwork).setOnClickListener(c);
        view.findViewById(R.id.syncPersistentConnection).setOnClickListener(c);
        view.findViewById(R.id.syncClearKey).setOnClickListener(c);

        com.lastutf445.home2.network.Sync.removeTrigger(com.lastutf445.home2.network.Sync.MENU_SYNC_TRIGGER);
        com.lastutf445.home2.network.Sync.addTrigger(
                com.lastutf445.home2.network.Sync.MENU_SYNC_TRIGGER,
                new Runnable() {
                    @Override
                    public void run() {
                        if (updater != null) {
                            updater.sendEmptyMessage(0);
                        }
                    }
                });

        updater.sendEmptyMessage(-1);
    }

    private void clearKey() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        builder.setTitle(res.getString(R.string.syncClearKeysCache));
        builder.setMessage(res.getString(R.string.syncClearKeysCacheMessage));

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DataLoader.setWithoutSync("PublicKeyModulus", "");
                DataLoader.setWithoutSync("PublicKeyExp", "");
                DataLoader.save();

                CryptoLoader.clearRSA();
                NotificationsLoader.makeToast("Cleared", true);
            }
        });

        builder.create().show();
    }

    @Override
    public void onDestroy() {
        try {
            com.lastutf445.home2.network.Sync.removeTrigger(
                    com.lastutf445.home2.network.Sync.MENU_SYNC_TRIGGER
            );
            updater = null;
        } catch (Exception e) {
            // lol
        }
        super.onDestroy();
    }

    @Override
    public void onResult(Bundle data) {
        updater.sendEmptyMessage(-1);
        UserLoader.setSettingsHandler(updater);
    }

    private static class Updater extends Handler {
        private WeakReference<View> weakView;

        Updater(View view) {
            this.weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case -1:
                    reload();
                    break;
                case 0:
                    updateNetworkState();
                    break;
                case 1:
                    unlockButton(msg.getData());
                    break;
            }
        }

        private void reload() {
            View view = weakView.get();

            if (view != null) {
                ((Switch) view.findViewById(R.id.syncPersistentConnectionSwitch)).setChecked(
                        DataLoader.getBoolean("SyncPersistentConnection", false)
                );
            }

            updateNetworkState();
        }

        private void updateNetworkState() {
            View view = weakView.get();
            if (view == null) return;

            TextView currentNetwork = view.findViewById(R.id.syncCurrentNetwork);
            CheckBox markAsHome = view.findViewById(R.id.syncMarkAsHomeNetworkCheckBox);

            String bssid = com.lastutf445.home2.network.Sync.getNetworkBSSID();
            boolean isHomeNetwork = DataLoader.getString("SyncHomeNetwork", "false").equals(bssid);
            int networkState = com.lastutf445.home2.network.Sync.getNetworkState();

            if (markAsHome != null) {
                markAsHome.setChecked(isHomeNetwork);
            }

            if (currentNetwork != null) {
                currentNetwork.setText(
                        networkState == 0 ? R.string.disconnected : isHomeNetwork ?
                                R.string.syncHomeNetwork : R.string.syncUnknownNetwork
                );
            }
        }

        private void unlockButton(@Nullable Bundle data) {
            if (data == null || !data.containsKey("id")) return;
            int id = data.getInt("id");
            View view = weakView.get();
            if (view == null) return;

            final View button = view.findViewById(id);
            if (button == null) return;

            Switch switcher = (Switch) ((ViewGroup) button).getChildAt(0);
            switcher.setChecked(!switcher.isChecked());

            final View spinner = ((ViewGroup) button).getChildAt(1);
            SimpleAnimator.fadeOut(spinner, 300, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    spinner.setVisibility(View.INVISIBLE);
                    button.setClickable(true);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }
    }
}
