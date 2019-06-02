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
import com.lastutf445.home2.fragments.dialog.Processing;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Account extends NavigationFragment {

    private Processing processing;
    private Updater updater;
    private Rename rename;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_account, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        rename = new Rename();
        processing = new Processing();
        updater = new Updater(view, rename, processing);
        UserLoader.setSettingsHandler(updater);

        processing.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(@NonNull DialogInterface dialog) {
                Sync.removeSyncProvider(Sync.PROVIDER_EDITOR);
                dialog.cancel();
            }
        });

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()) {
                    case R.id.accountEditUsername:
                        rename();
                        break;
                    case R.id.accountPrivacy:
                        FragmentsLoader.addChild(new Privacy(), Account.this);
                        break;
                    case R.id.accountLogout:
                        logout();
                        break;
                }
            }
        };

        view.findViewById(R.id.accountEditUsername).setOnClickListener(c);
        view.findViewById(R.id.accountPrivacy).setOnClickListener(c);
        view.findViewById(R.id.accountLogout).setOnClickListener(c);
        updater.sendEmptyMessage(-1);
    }

    private void rename() {
        rename.setTitle(DataLoader.getAppResources().getString(R.string.accountUsernameTitle));
        rename.setHint(DataLoader.getAppResources().getString(R.string.accountUsernameHint));
        rename.setOld(UserLoader.getUsername());

        rename.setCallback(new Rename.Callback() {
            @Override
            public boolean onApply(@NonNull String s) {
                String t = s.trim();

                if (t.length() == 0 || t.equals("None")) {
                    NotificationsLoader.makeToast("Invalid account name", true);
                    return false;

                } else {
                    NotificationsLoader.makeToast("Success", true);
                    if (UserLoader.isAuthenticated()) {
                        DataLoader.set("Username", t);
                    } else {
                        DataLoader.setWithoutSync("UserName", t);
                    }
                    DataLoader.save();
                    return true;
                }
            }
        });

        FragmentsLoader.addChild(rename, Account.this);
    }

    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        builder.setTitle(res.getString(R.string.accountLogout));
        builder.setMessage(res.getString(R.string.accountLogoutMessage));

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.accountLogout, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UserLoader.logout();

                NotificationsLoader.makeToast(
                        DataLoader.getAppResources().getString(R.string.success),
                        true
                );

                toParent.putBoolean("reload", true);
                getActivity().onBackPressed();
            }
        });

        builder.create().show();
    }

    @Override
    public void onResult(Bundle data) {
        updater.sendEmptyMessage(-1);
        UserLoader.setSettingsHandler(updater);
    }

    private static class Updater extends Handler {
        private WeakReference<Processing> weakProcessing;
        private WeakReference<Rename> weakRename;
        private WeakReference<View> weakView;

        public Updater(View view, Rename rename, Processing processing) {
            weakProcessing = new WeakReference<>(processing);
            weakRename = new WeakReference<>(rename);
            weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case -1:
                    reload();
                    break;
                case 0:
                    finish(R.string.disconnected);
                    break;
                case 1:
                    setTitle(R.string.processing);
                    break;
                case 2:
                    finish(R.string.masterServerRequired);
                    break;
                case 3:
                    setTitle(R.string.waitingForAConnection);
                    break;
                case 4:
                    finish(R.string.encryptionError);
                    break;
                case 5:
                    done();
                    break;
            }
        }

        private void reload() {
            View view = weakView.get();

            if (view != null) {
                ((TextView) view.findViewById(R.id.accountUsername)).setText(
                        UserLoader.getUsername()
                );
            }

            Rename rename = weakRename.get();

            if (rename != null) {
                rename.setOld(
                        UserLoader.getUsername()
                );
            }
        }

        private void done() {
            NotificationsLoader.makeToast(
                    DataLoader.getAppResources().getString(R.string.success),
                    true
            );

            closeDialog();

            Rename rename = weakRename.get();

            if (rename != null) {
                rename.getActivity().onBackPressed();
            }
        }

        private void finish(int title) {
            if (title != 0) {
                NotificationsLoader.makeToast(
                        DataLoader.getAppResources().getString(title),
                        true
                );
            }

            closeDialog();
        }

        private void setTitle(int title) {
            Processing dialog = weakProcessing.get();

            if (dialog != null) {
                dialog.setTitle(
                        DataLoader.getAppResources().getString(title)
                );
            }
        }

        private void closeDialog() {
            Processing dialog = weakProcessing.get();

            if (dialog != null) {
                dialog.getDialog().cancel();
            }
        }
    }
}
