package com.lastutf445.home2.fragments.menu;

import android.content.DialogInterface;
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
        UserLoader.logout();

        NotificationsLoader.makeToast(
                DataLoader.getAppResources().getString(R.string.success),
                true
        );

        toParent.putBoolean("reload", true);
        getActivity().onBackPressed();

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
            Processing dialog = weakProcessing.get();

            switch (msg.what) {
                case 0:
                    unexpectedError(dialog);
                    break;
                case 1:
                    processing(dialog);
                    break;
                case 2:
                    done(dialog);
                    break;
            }
        }

        private void unexpectedError(@Nullable Processing dialog) {
            NotificationsLoader.makeToast(
                    DataLoader.getAppResources().getString(R.string.unexpectedError),
                    true
            );

            closeDialog(dialog);
        }

        private void processing(Processing dialog) {
            if (dialog != null) {
                dialog.setTitle(
                        DataLoader.getAppResources().getString(R.string.processing)
                );
            }
        }

        private void done(@Nullable Processing dialog) {
            NotificationsLoader.makeToast(
                    DataLoader.getAppResources().getString(R.string.success),
                    true
            );

            closeDialog(dialog);

            Rename rename = weakRename.get();

            if (rename != null) {
                rename.getActivity().onBackPressed();
            }
        }

        private void closeDialog(@Nullable Processing dialog) {
            if (dialog != null) {
                dialog.getDialog().cancel();
            }
        }
    }
}
