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
import android.view.animation.Animation;
import android.widget.Switch;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.Ping;
import com.lastutf445.home2.util.SimpleAnimator;

import org.json.JSONException;

import java.lang.ref.WeakReference;

public class Module extends NavigationFragment {

    private com.lastutf445.home2.containers.Module module;
    private int pos;

    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.module, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        if (module == null) return;

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.moduleConnection:
                        ping();
                        break;
                    case R.id.moduleEditTitle:
                        editTitle();
                        break;
                    case R.id.moduleConfigure:
                        configureSpecial();
                        break;
                    case R.id.moduleSync:
                        switchSync();
                        break;
                    case R.id.moduleDelete:
                        delete();
                        break;
                }
            }
        };

        view.findViewById(R.id.moduleConnection).setOnClickListener(c);
        view.findViewById(R.id.moduleEditTitle).setOnClickListener(c);
        view.findViewById(R.id.moduleConfigure).setOnClickListener(c);
        view.findViewById(R.id.moduleSync).setOnClickListener(c);
        view.findViewById(R.id.moduleDelete).setOnClickListener(c);
        updater = new Updater(view, module);

        ModulesLoader.setModuleUpdater(module.getSerial(), updater);
        reload();
    }

    @Override
    protected void reload() {
        ((TextView) view.findViewById(R.id.moduleTitle)).setText(
                module.getTitle()
        );

        ((TextView) view.findViewById(R.id.moduleType)).setText(
                module.getStyledType()
        );

        if (module.getIp() != null) {
            ((TextView) view.findViewById(R.id.moduleIPAddress)).setText(
                    module.getIp().getHostAddress()
            );
        }

        ((TextView) view.findViewById(R.id.moduleSerial)).setText(
                String.valueOf(module.getSerial())
        );

        ((Switch) view.findViewById(R.id.moduleSyncCheckBox)).setChecked(
                module.getSyncing()
        );
    }

    public void setModule(@NonNull com.lastutf445.home2.containers.Module module, int pos) {
        this.module = module;
        this.pos = pos;
    }

    private void ping() {
        if (module.getIp() != null) {
            if (!UserLoader.isAuthenticated()) {
                ((TextView) view.findViewById(R.id.moduleConnection)).setText(
                        DataLoader.getAppResources().getString(R.string.authenticationRequiredSmall)
                );
                return;
            }

            try {
                Ping ping = new Ping(module.getIp(), module.getPort());
                ping.setHandler(updater);

                updater.sendEmptyMessage(-2);
                Sync.addSyncProvider(ping);
                return;

            } catch (JSONException e) {
                //e.printStackTrace();
            }
        }

        ((TextView) view.findViewById(R.id.moduleConnection)).setText(
                DataLoader.getAppResources().getString(R.string.unknownAddressSmall)
        );
    }

    private void editTitle() {
        Rename rename = new Rename();
        Resources res = DataLoader.getAppResources();
        rename.setTitle(res.getString(R.string.moduleTitle));
        rename.setHint(res.getString(R.string.moduleHint));
        rename.setOld(module.getTitle());

        rename.setCallback(new Rename.Callback() {
            @Override
            public boolean onApply(@NonNull String s) {
                String t = s.trim();

                if (t.length() == 0) {
                    NotificationsLoader.makeToast("Invalid module title", true);
                    return false;

                } else {
                    NotificationsLoader.makeToast("Success", true);
                    toParent.putInt("updated", pos);
                    module.setTitle(t);
                    return true;
                }
            }
        });

        FragmentsLoader.addChild(rename, Module.this);
    }

    private void configureSpecial() {
        ModulesLoader.configure(1, module, this);
    }

    private void switchSync() {
        if (!UserLoader.isAuthenticated()) {
            NotificationsLoader.makeToast(
                    DataLoader.getAppResources().getString(R.string.authenticationRequired),
                    true
            );
            return;
        }

        View button = view.findViewById(R.id.moduleSync);
        button.setClickable(false);

        View spinner = ((ViewGroup) button).getChildAt(2);
        SimpleAnimator.fadeIn(spinner, 300);

        //module.setSyncing(!module.getSyncing());
        ModulesLoader.onModuleSyncingChanged(module, !module.getSyncing(), updater);
        //toParent.putBoolean("syncStateChanged", true);
        //updater.sendEmptyMessage(0);
    }

    private void delete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        builder.setTitle(res.getString(R.string.modulesConfigDialogTitle));
        builder.setMessage(res.getString(R.string.modulesConfigDialogMessage));

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ModulesLoader.removeModule(module);
                toParent.putInt("deleted", pos);
                NotificationsLoader.makeToast("Deleted", true);
                getActivity().onBackPressed();
            }
        });

        builder.create().show();
    }

    @Override
    public void onResult(@NonNull Bundle data) {
        if (data.containsKey("reload")) {
            toParent.putInt("updated", pos);
            reload();
        }
    }

    @Override
    public void onDestroy() {
        try {
            Sync.removeSyncProvider(Sync.PROVIDER_PING);
            ModulesLoader.resetUpdater();
        } catch (Exception e) {
            // lol
        }
        super.onDestroy();
    }

    private static class Updater extends Handler {
        private WeakReference<com.lastutf445.home2.containers.Module> weakModule;
        private WeakReference<View> weakView;

        Updater(View view, com.lastutf445.home2.containers.Module module) {
            this.weakView = new WeakReference<>(view);
            this.weakModule = new WeakReference<>(module);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case -3:
                    subscribeStatusUpdate();
                    break;
                case -2:
                    beginPing();
                    break;
                case -1:
                    updateConnectionStatus(msg.getData());
                    break;
                case 0:
                    unlockSyncButton(msg.getData());
                    break;
            }
        }

        private void subscribeStatusUpdate() {
            View view = weakView.get();
            if (view == null) return;

            Switch switcher = view.findViewById(R.id.moduleSyncCheckBox);
            com.lastutf445.home2.containers.Module module = weakModule.get();

            if (module != null) {
                switcher.setChecked(module.getSyncing());
            }
        }

        private void beginPing() {
            View view = weakView.get();
            if (view == null) return;

            SimpleAnimator.fadeIn(view.findViewById(R.id.moduleSpinner), 300);
            TextView connection = view.findViewById(R.id.moduleConnection);

            connection.setText(
                    DataLoader.getAppResources().getString(R.string.pending)
            );

            connection.setClickable(false);
        }

        private void updateConnectionStatus(@Nullable Bundle data) {
            View view = weakView.get();
            if (view == null) return;

            TextView connection = view.findViewById(R.id.moduleConnection);

            connection.setText(
                    data == null ? R.string.unexpectedError :
                            data.getInt("status", R.string.unexpectedError)
            );

            final View spinner = view.findViewById(R.id.moduleSpinner);
            connection.setClickable(true);

            SimpleAnimator.fadeOut(spinner, 300, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    spinner.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }

        private void unlockSyncButton(@Nullable Bundle data) {
            View view = weakView.get();
            if (view == null) return;

            if (data == null) {
                data = new Bundle();
            }

            View button = view.findViewById(R.id.moduleSync);
            button.setClickable(true);

            Switch switcher = (Switch) ((ViewGroup) button).getChildAt(1);
            com.lastutf445.home2.containers.Module module = weakModule.get();

            if (module != null && !data.containsKey("status")) {
                switcher.setChecked(!switcher.isChecked());
                module.setSyncing(!module.getSyncing());

            } else {
                NotificationsLoader.makeToast(
                        DataLoader.getAppResources().getString(
                                data.getInt("status", R.string.unexpectedError)
                        ),
                        true
                );
            }

            final View spinner = ((ViewGroup) button).getChildAt(2);
            SimpleAnimator.fadeOut(spinner, 300, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    spinner.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }
    }
}
