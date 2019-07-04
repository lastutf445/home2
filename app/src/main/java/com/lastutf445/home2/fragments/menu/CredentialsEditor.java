package com.lastutf445.home2.fragments.menu;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.fragments.dialog.Processing;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class CredentialsEditor extends NavigationFragment {

    private boolean pwdNeeded;
    private String hint, desc, field;
    private Verifier verifier;
    private int inputType;

    private TextView tvDesc;
    private EditText password;
    private EditText value;

    private Processing processing;
    private Updater updater;
    private Editor editor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.credentials_editor, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(this);

        try {
            editor = new Editor(updater);

        } catch (JSONException e) {
            NotificationsLoader.makeToast("Unexpected error", true);
            getActivity().onBackPressed();
            //e.printStackTrace();
        }

        processing = new Processing();
        processing.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Sync.removeSyncProvider(Sync.PROVIDER_CREDENTIALS_EDITOR);
                dialog.cancel();
            }
        });

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t = value.getText().toString().trim();
                String p = password.getText().toString().trim();
                MainActivity.hideKeyboard();

                if (pwdNeeded) {
                    if (p.length() == 0) {
                        NotificationsLoader.makeToast("Password needed", true);
                        return;
                    }
                }

                if (verifier == null) {
                    NotificationsLoader.makeToast("Unable to start verifier", true);

                } else {
                    verifier.onSend(t);
                }

            }
        };

        view.findViewById(R.id.credentialsSave).setOnClickListener(c);

        password = view.findViewById(R.id.credentialsPasswordCheck);
        value = view.findViewById(R.id.credentialsValue);
        tvDesc = view.findViewById(R.id.credentialsDesc);
        reload();
    }

    @Override
    protected void reload() {
        value.setInputType(inputType);
        value.setHint(hint);

        if (desc.length() == 0) {
            tvDesc.setVisibility(View.GONE);

        } else {
            tvDesc.setVisibility(View.VISIBLE);
            tvDesc.setText(desc);
        }

        if (pwdNeeded) {
            password.setVisibility(View.VISIBLE);

        } else {
            password.setVisibility(View.GONE);
        }
    }

    public void setPwdNeeded(boolean pwdNeeded) {
        this.pwdNeeded = pwdNeeded;

        if (!pwdNeeded) {
            password.setText("");
        }
    }

    public void setHint(@StringRes int hint) {
        this.hint = DataLoader.getAppResources().getString(hint);
    }

    public void setDesc(@StringRes int desc) {
        if (desc == 0) {
            this.desc = "";

        } else {
            this.desc = DataLoader.getAppResources().getString(desc);
        }
    }

    public void setField(@NonNull String field) {
        this.field = field;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public void setVerifier(@NonNull Verifier verifier) {
        this.verifier = verifier;
    }

    public void send() {
        String t = value.getText().toString().trim();
        String p = password.getText().toString().trim();

        if (processing.isInactive()) {
            processing.show(getChildFragmentManager(), "processing");
        }

        editor.setValue(field, t, p);
        Sync.addSyncProvider(editor);
    }

    public static class Updater extends Handler {
        private WeakReference<CredentialsEditor> weakEditor;

        public Updater(@NonNull CredentialsEditor editor) {
            weakEditor = new WeakReference<>(editor);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    end(msg.getData().getInt("status"));
                    break;
                case 1:
                    result(msg.getData());
                    break;
            }
        }

        private void result(Bundle data) {
            CredentialsEditor editor = weakEditor.get();
            if (editor == null) return;

            int status = data.getInt("status");

            if (status == Sync.UNKNOWN_USER) {
                end(R.string.credentialsPasswordCheckFailed);
                return;
            }

            if (status == Sync.LOGIN_IS_ALREADY_TAKEN) {
                end(R.string.credentialsLoginIsTaken);
                return;
            }

            if (status != Sync.OK) {
                end(R.string.unexpectedError);

            } else {
                end(R.string.success);
            }
        }

        private void end(int status) {
            CredentialsEditor editor = weakEditor.get();

            if (status != 0) {
                NotificationsLoader.makeToast(
                        DataLoader.getAppResources().getString(status),
                        true
                );
            }

            if (editor != null && editor.processing != null) {
                if (!editor.processing.isInactive()) {
                    editor.processing.dismiss();
                }
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            MainActivity.hideKeyboard();
        }
    }

    @Override
    public void onDestroy() {
        Sync.removeSyncProvider(Sync.PROVIDER_CREDENTIALS_EDITOR);
        super.onDestroy();
    }

    public class Editor extends SyncProvider {
        private WeakReference<Updater> weakUpdater;
        private String field, value, passwordCheck;
        private boolean tainted = false;

        public Editor(@NonNull Updater updater) throws JSONException {
            super(
                    Sync.PROVIDER_CREDENTIALS_EDITOR,
                    "credentialsEditor",
                    new JSONObject(),
                    null,
                    Sync.DEFAULT_PORT,
                    false);

            weakUpdater = new WeakReference<>(updater);
        }

        @Override
        public boolean isWaiting() {
            return field == null;
        }

        public synchronized void setValue(@NonNull String field, @NonNull String value, @Nullable String passwordCheck) {
            this.passwordCheck = passwordCheck;
            this.field = field;
            this.value = value;
            this.tainted = true;
        }

        @Override
        public void onPostPublish(int statusCode) {
            if (statusCode < 0 || statusCode == 1) return;
            Updater updater = weakUpdater.get();
            Bundle data = new Bundle();
            int status;

            if (updater == null) return;

            switch (statusCode) {
                case 2:
                    status = R.string.masterServerRequired;
                    break;
                case 3:
                    status = R.string.disconnected;
                    break;
                case 4:
                    status = R.string.encryptionError;
                    break;
                default:
                    status = R.string.unexpectedError;
                    break;
            }

            data.putInt("status", status);

            Message msg = updater.obtainMessage(0);
            msg.setData(data);
            updater.sendMessage(msg);
        }

        @Override
        public void onReceive(JSONObject data) {
            Updater updater = weakUpdater.get();
            if (updater == null) return;

            Message msg = updater.obtainMessage(1);
            Bundle msgData = new Bundle();

            try {
                msgData.putInt("status", data.getInt("status"));
                Sync.removeSyncProvider(Sync.PROVIDER_CREDENTIALS_EDITOR);

            } catch (JSONException e) {
                //e.printStackTrace();
                msgData.putInt("status", Sync.UNEXPECTED_ERROR);
                msg.what = 0;
            }

            msg.setData(msgData);
            updater.sendMessage(msg);
        }

        @Override
        public JSONObject getQuery() {
            if (tainted) {
                try {
                    JSONObject data = new JSONObject();
                    data.put(field, value);

                    if (passwordCheck != null && passwordCheck.length() != 0) {
                        data.put("pwdCheck", passwordCheck);
                    }

                    query.put("data", data);
                    tainted = false;

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return query;
        }
    }

    public interface Verifier {
        void onSend(@NonNull String value);
    }
}
