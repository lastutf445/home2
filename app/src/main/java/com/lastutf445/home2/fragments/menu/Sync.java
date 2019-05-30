package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.MessagesLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
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

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.syncMarkAsHomeNetwork:
                        String bssid = com.lastutf445.home2.network.Sync.getNetworkBSSID();

                        if (com.lastutf445.home2.network.Sync.getNetworkState() == 2) {
                            if(!DataLoader.getString("SyncHomeNetwork", "false").equals(bssid)) {
                                DataLoader.set("SyncHomeNetwork", bssid);
                            }
                            else {
                                DataLoader.set("SyncHomeNetwork", null);
                            }

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
                }
            }
        };

        View.OnClickListener d = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setClickable(false);

                View spinner = ((ViewGroup) v).getChildAt(1);
                SimpleAnimator.fadeIn(spinner, 300);

                Bundle data = new Bundle();
                GlobalSyncSwitch syncSwitch = new GlobalSyncSwitch(
                        updater,
                        data
                );

                switch (v.getId()) {
                    case R.id.syncDashboard:
                        data.putInt("id", R.id.syncDashboard);
                        syncSwitch.setTask(new ModulesLoader.SyncSwitch());
                        break;
                    case R.id.syncMessages:
                        data.putInt("id", R.id.syncMessages);
                        syncSwitch.setTask(new MessagesLoader.SyncSwitch());
                        break;
                    case R.id.syncNotifications:
                        data.putInt("id", R.id.syncNotifications);
                        syncSwitch.setTask(new NotificationsLoader.SyncSwitch());
                        break;
                }

                new Thread(syncSwitch).start();
            }
        };

        view.findViewById(R.id.syncBehavior).setOnClickListener(c);
        view.findViewById(R.id.syncMasterServer).setOnClickListener(c);
        view.findViewById(R.id.syncExternalConnection).setOnClickListener(c);
        view.findViewById(R.id.syncMarkAsHomeNetwork).setOnClickListener(c);

        view.findViewById(R.id.syncDashboard).setOnClickListener(d);
        view.findViewById(R.id.syncMessages).setOnClickListener(d);
        view.findViewById(R.id.syncNotifications).setOnClickListener(d);

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

        reload();
    }

    @Override
    protected void reload() {
        ((Switch) view.findViewById(R.id.syncDashboardSwitch)).setChecked(
                DataLoader.getBoolean("SyncDashboard", false)
        );

        ((Switch) view.findViewById(R.id.syncMessagesSwitch)).setChecked(
                DataLoader.getBoolean("SyncMessages", false)
        );

        ((Switch) view.findViewById(R.id.syncNotificationsSwitch)).setChecked(
                DataLoader.getBoolean("SyncNotifications", false)
        );

        ((TextView) view.findViewById(R.id.syncHomeNetwork)).setText(
                String.format("%s %s", DataLoader.getAppResources().getString(R.string.syncHomeNetwork),
                        DataLoader.getString("SyncHomeNetwork",
                                DataLoader.getAppResources().getString(R.string.undefined)))
        );

        updater.sendEmptyMessage(0);
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

    private static class Updater extends Handler {
        private WeakReference<View> weakView;

        Updater(View view) {
            this.weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    updateNetworkState();
                    break;
                case 1:
                    unlockButton(msg.getData());
                    break;
            }
        }

        private void updateNetworkState() {
            View view = weakView.get();
            if (view == null) return;

            TextView currentNetwork = view.findViewById(R.id.syncCurrentNetwork);
            CheckBox markAsHome = view.findViewById(R.id.syncMarkAsHomeNetworkCheckBox);

            if (markAsHome != null) {
                markAsHome.setChecked(
                        DataLoader.getString("SyncHomeNetwork", "false").equals(com.lastutf445.home2.network.Sync.getNetworkBSSID())
                );
            }

            if (currentNetwork != null) {
                currentNetwork.setText(String.format("%s %s",
                        DataLoader.getAppResources().getString(R.string.syncCurrentNetwork),
                        com.lastutf445.home2.network.Sync.getNetworkState() == 2 ? com.lastutf445.home2.network.Sync.getNetworkBSSID() :
                                DataLoader.getAppResources().getString(R.string.BSSIDError)
                ));
            }
        }

        private void unlockButton(Bundle data) {
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

    private static class GlobalSyncSwitch implements Runnable {
        private Updater updater;
        private Runnable task;
        private Bundle data;

        GlobalSyncSwitch(Updater updater, Bundle data) {
            this.updater = updater;
            this.data = data;
        }

        public void setTask(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            Thread thread = new Thread(task);
            thread.start();

            try {
                thread.sleep(300);
                thread.join();

            } catch (InterruptedException e) {
                //e.printStackTrace();
            }

            Message msg = updater.obtainMessage(1);
            msg.setData(data);
            updater.sendMessage(msg);
        }
    }
}
