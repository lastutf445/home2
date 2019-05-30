package com.lastutf445.home2.fragments.menu;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.lastutf445.home2.R;
import com.lastutf445.home2.fragments.dialog.Processing;
import com.lastutf445.home2.loaders.CryptoLoader;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Auth extends NavigationFragment {

    private Processing processing;
    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_auth, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(view, toParent, getActivity());
        processing = new Processing();

        processing.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Sync.removeSyncProvider(Sync.PROVIDER_CREDENTIALS);
                Sync.removeSyncProvider(Sync.PROVIDER_GET_PUBLIC_KEY);
                dialog.cancel();
            }
        });

        updater.setProcessing(processing);

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.authEnter:
                        enter();
                        break;
                    case R.id.authEnterBasic:
                        enterBasic();
                        break;
                }
            }
        };

        view.findViewById(R.id.authEnter).setOnClickListener(c);
        view.findViewById(R.id.authEnterBasic).setOnClickListener(c);
    }

    private void enter() {
        String login = ((EditText) view.findViewById(R.id.authLogin)).getText().toString();
        String password = ((EditText) view.findViewById(R.id.authPassword)).getText().toString();

        if (login.length() == 0 || password.length() == 0) {
            NotificationsLoader.makeToast(DataLoader.getAppResources().getString(R.string.missedField), true);

        } else {
            processing.setTitle(DataLoader.getAppResources().getString(R.string.waitingForAConnection));
            processing.show(getActivity().getSupportFragmentManager(), "processing");
            UserLoader.startAuth(login, password, updater);
        }
    }

    private void enterBasic() {
        UserLoader.authBasic();

        NotificationsLoader.makeToast(
                DataLoader.getAppResources().getString(R.string.success),
                true
        );

        toParent.putBoolean("reload", true);
        getActivity().onBackPressed();
    }

    @Override
    public void onDestroy() {
        try {
            Sync.removeSyncProvider(Sync.PROVIDER_GET_PUBLIC_KEY);
            Sync.removeSyncProvider(Sync.PROVIDER_CREDENTIALS);
            CryptoLoader.clearRSA();
        } catch (Exception e) {
            // lol
        }
        super.onDestroy();
    }

    private static class Updater extends Handler {
        private WeakReference<View> weakView;
        private WeakReference<Bundle> weakToParent;
        private WeakReference<Activity> weakActivity;
        private WeakReference<Processing> weakProcessing;

        public Updater(@NonNull View view, @NonNull Bundle toParent, Activity activity) {
            weakView = new WeakReference<>(view);
            weakToParent = new WeakReference<>(toParent);
            weakActivity = new WeakReference<>(activity);
        }

        public void setProcessing(@NonNull Processing processing) {
            weakProcessing = new WeakReference<>(processing);
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
                    setTitle(R.string.authRequestingPubKey);
                    break;
                case 6:
                    finish(R.string.authInvalidPubKey);
                    break;
                case 7:
                    setTitle(R.string.authSendingCredentials);
                    break;
                case 8:
                    finish(R.string.authInvalidCredentials);
                    break;
                case 9:
                    ok();
                    break;
            }
        }

        private void ok() {
            finish(R.string.success);

            Bundle toParent = weakToParent.get();
            Activity activity = weakActivity.get();

            if (toParent != null) {
                toParent.putBoolean("reload", true);
            }

            if (activity != null) {
                activity.onBackPressed();
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

            if (dialog != null && dialog.getDialog() != null) {
                dialog.getDialog().cancel();
            }
        }
    }
}
