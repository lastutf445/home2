package com.lastutf445.home2.fragments.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
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
        updater = new Updater(view);

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
            public boolean onApply(String s) {
                String t = s.trim();

                if (t.length() == 0) {
                    NotificationsLoader.makeToast("Invalid module title", true);
                    return false;

                } else {
                    NotificationsLoader.makeToast("Success", true);
                    toParent.putBoolean("updated", true);
                    module.setTitle(t);
                    return true;
                }
            }
        });

        FragmentsLoader.addChild(rename, Module.this);
    }

    private void configureSpecial() {
        ModulesLoader.callSpecial(1, module, this);
    }

    private void switchSync() {
        View button = view.findViewById(R.id.moduleSync);
        button.setClickable(false);

        View spinner = ((ViewGroup) button).getChildAt(2);
        SimpleAnimator.fadeIn(spinner, 300);

        module.setSyncing(!module.getSyncing());
        toParent.putBoolean("syncStateChanged", true);
        updater.sendEmptyMessage(0);
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
            public void onClick(DialogInterface dialog, int which) {
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
    public void onResult(Bundle data) {
        if (data.containsKey("reload")) {
            toParent.putInt("updated", pos);
            reload();
        }
    }

    @Override
    public void onDestroy() {
        Sync.removeSyncProvider(Sync.PROVIDER_PING);
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
                case -2:
                    beginPing();
                    break;
                case -1:
                    updateConnectionStatus(msg.getData());
                    break;
                case 0:
                    unlockSyncButton();
                    break;
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

        private void updateConnectionStatus(Bundle data) {
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

        private void unlockSyncButton() {
            View view = weakView.get();
            if (view == null) return;

            View button = view.findViewById(R.id.moduleSync);
            button.setClickable(true);

            Switch switcher = (Switch) ((ViewGroup) button).getChildAt(1);
            switcher.setChecked(!switcher.isChecked());

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
