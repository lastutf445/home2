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
import com.lastutf445.home2.loaders.NodesLoader;
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

        processing.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
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
        reload();
    }

    @Override
    protected void reload() {
        ((TextView) view.findViewById(R.id.accountUsername)).setText(
                UserLoader.getUsername()
        );
    }

    private void rename() {
        rename.setTitle(DataLoader.getAppResources().getString(R.string.accountUsernameTitle));
        rename.setHint(DataLoader.getAppResources().getString(R.string.accountUsernameHint));
        rename.setOld(UserLoader.getUsername());

        rename.setCallback(new Rename.Callback() {
            @Override
            public boolean onApply(String s) {
                String t = s.trim();

                if (t.length() == 0) {
                    NotificationsLoader.makeToast("Invalid account name", true);
                    return false;

                } else if (DataLoader.getBoolean("BasicAccount", false)) {
                    renameOffline(t);
                    return true;

                } else {
                    processing.setTitle(
                            DataLoader.getAppResources().getString(R.string.waitingForAConnection)
                    );

                    processing.show(getActivity().getSupportFragmentManager(), "processing");
                    UserLoader.setOnlineUsername(t, updater);
                    return false;
                }
            }
        });

        FragmentsLoader.addChild(rename, Account.this);
    }

    private void renameOffline(@NonNull String s) {
        NotificationsLoader.makeToast("Success", true);
        DataLoader.set("Username", s);
        DataLoader.save();
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
            public void onClick(DialogInterface dialog, int which) {
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
        reload();
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
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    finish(R.string.unexpectedError);
                    break;
                case 1:
                    setTitle(R.string.processing);
                    break;
                case 2:
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
