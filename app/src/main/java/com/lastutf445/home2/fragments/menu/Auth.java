package com.lastutf445.home2.fragments.menu;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.fragments.dialog.Processing;
import com.lastutf445.home2.loaders.CryptoLoader;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Auth extends NavigationFragment {

    private boolean falseCancellation;
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
        updater = new Updater(this, toParent, getActivity());
        processing = new Processing();

        processing.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(@NonNull DialogInterface dialog) {
                if (!falseCancellation) {
                    UserLoader.cancelAuth();
                }

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
                    case R.id.authEnterByEmail:
                        enterByEmail();
                        break;
                }
            }
        };

        view.findViewById(R.id.authEnter).setOnClickListener(c);
        view.findViewById(R.id.authEnterByEmail).setOnClickListener(c);
    }

    private void enter() {
        String login = ((EditText) view.findViewById(R.id.authLogin)).getText().toString();
        String password = ((EditText) view.findViewById(R.id.authPassword)).getText().toString();

        if (login.length() == 0 || password.length() == 0) {
            NotificationsLoader.makeToast(DataLoader.getAppResources().getString(R.string.missedField), true);

        } else {
            processing.setTitle(DataLoader.getAppResources().getString(R.string.waitingForAConnection));
            processing.show(getChildFragmentManager(), "processing");
            UserLoader.startAuth(login, password, updater);
        }
    }

    private void enterByEmail() {
        final EnterByEmail restoreAccess = new EnterByEmail();
        restoreAccess.setConnector(new EnterByEmail.NoPopConnector() {
            @Override
            public void onPop() {
                toParent.putBoolean("reload", true);
                getParent().setChild(restoreAccess);
                restoreAccess.setParent(getParent());
                restoreAccess.setConnector(null);
                FragmentsLoader.removeFragment2(Auth.this);
                getActivity().onBackPressed();

            }
        });

        FragmentsLoader.addChild(restoreAccess, this);
    }

    @Override
    public void onResult(Bundle data) {
        if (data.containsKey("authKey")) {
            String key = data.getString("authKey", "");

            if (key.length() != 0) {
                if (processing.isInactive()) {
                    processing.show(getChildFragmentManager(), "processing");
                }

                UserLoader.setAuthKey(key);

            } else {
                UserLoader.cancelAuth();
            }
        }
    }

    @Override
    public void onDestroy() {
        MainActivity.hideKeyboard();

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
        private WeakReference<Auth> weakAuth;
        private WeakReference<Bundle> weakToParent;
        private WeakReference<Activity> weakActivity;
        private WeakReference<Processing> weakProcessing;

        public Updater(@NonNull Auth auth, @NonNull Bundle toParent, Activity activity) {
            weakAuth = new WeakReference<>(auth);
            weakToParent = new WeakReference<>(toParent);
            weakActivity = new WeakReference<>(activity);
        }

        public void setProcessing(@NonNull Processing processing) {
            weakProcessing = new WeakReference<>(processing);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
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
                case 10:
                    setTitle(R.string.checkingIntegrity);
                    break;
                case 11:
                    getAuthKey();
                    break;
            }
        }

        private void getAuthKey() {
            Auth auth = weakAuth.get();
            if (auth == null) return;

            auth.falseCancellation = true;

            if (!auth.processing.isInactive() && auth.processing.getDialog() != null) {
                auth.processing.dismiss();
            }

            AuthKey authKey = new AuthKey();
            FragmentsLoader.addChild(authKey, auth);
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
            Auth auth = weakAuth.get();
            Processing dialog = weakProcessing.get();

            if (auth != null) {
                auth.falseCancellation = false;
            }

            if (dialog != null && dialog.getDialog() != null) {
                dialog.getDialog().cancel();
            }
        }
    }
}
