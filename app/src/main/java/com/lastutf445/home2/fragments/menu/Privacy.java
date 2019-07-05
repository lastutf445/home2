package com.lastutf445.home2.fragments.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

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
import com.lastutf445.home2.util.SimpleAnimator;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public class Privacy extends NavigationFragment {

    private RadioGroup.OnCheckedChangeListener d;
    private ArrayList<Character> alphabet;
    private RadioGroup radioGroup;
    private Switch allowAltAuth;
    private Updater updater;

    private boolean startedChangingAESKey = false;
    private UserLoader.KeyChanger keyChanger;
    private TextView accountAESLength;
    private Processing processing;
    private String key;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_privacy, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.accountGenAES:
                        generateAES();
                        break;
                    case R.id.accountPassword:
                        password();
                        break;
                    case R.id.accountLogin:
                        login();
                        break;
                    case R.id.allowAltAuth:
                        altAuth();
                        break;
                    case R.id.changeEmail:
                        email();
                        break;
                    case R.id.closeSessions:
                        closeSessions();
                        break;
                }
            }
        };

        d = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.accountKey128:
                        DataLoader.set("AESBytes", 16);
                        break;
                    case R.id.accountKey256:
                        DataLoader.set("AESBytes", 32);
                        break;
                }

                DataLoader.save();
            }
        };

        alphabet = new ArrayList<>();
        for (char i: "abcdefghijklmnopqrstuvwxyz".toCharArray()) {
            alphabet.add(i);
        }

        updater = new Updater(this);
        radioGroup = view.findViewById(R.id.accountKeyLength);
        allowAltAuth = view.findViewById(R.id.allowAltAuthSwitch);
        accountAESLength = view.findViewById(R.id.accountAESLength);
        UserLoader.setSettingsHandler(updater);

        processing = new Processing();
        processing.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Sync.removeSyncProvider(Sync.PROVIDER_TERMINATE_SESSIONS);

                if (startedChangingAESKey) {
                    Sync.removeSyncProvider(Sync.PROVIDER_KEY_CHANGER);
                }

                if (dialog != null) {
                    dialog.cancel();
                }
            }
        });

        radioGroup.check(
                DataLoader.getInt("AESBytes", 16) == 16 ? R.id.accountKey128 :
                        R.id.accountKey256
        );

        allowAltAuth.setChecked(
                DataLoader.getBoolean("AllowAltAuth", true)
        );

        accountAESLength.setText(
                String.format(
                        Locale.UK,
                        "%s %d bits",
                        DataLoader.getAppResources().getString(R.string.accountEncryptionAESLength),
                        CryptoLoader.getInstalledAESKeyLength() * 8
                )
        );

        view.findViewById(R.id.accountGenAES).setOnClickListener(c);
        view.findViewById(R.id.accountPassword).setOnClickListener(c);
        view.findViewById(R.id.accountLogin).setOnClickListener(c);
        view.findViewById(R.id.allowAltAuth).setOnClickListener(c);
        view.findViewById(R.id.changeEmail).setOnClickListener(c);
        view.findViewById(R.id.closeSessions).setOnClickListener(c);
        radioGroup.setOnCheckedChangeListener(d);

        SimpleAnimator.drawableTint(
                (Button) view.findViewById(R.id.accountGenAES),
                Color.parseColor("#666666")
        );

        SimpleAnimator.drawableTint(
                (Button) view.findViewById(R.id.closeSessions),
                Color.parseColor("#AD1457")
        );
    }

    private void generateAES() {
        try {
            if (Sync.hasSyncProvider(Sync.PROVIDER_KEY_CHANGER)) {
                NotificationsLoader.makeToast("Key is already changing now, please wait", true);
                return;
            }

            key = CryptoLoader.createAESKey();
            keyChanger = new UserLoader.KeyChanger(updater, key);
            Sync.addSyncProvider(keyChanger);
            startedChangingAESKey = true;

            processing.setTitle(
                    DataLoader.getAppResources().getString(
                            R.string.keyChanging
                    )
            );

            getChildFragmentManager().beginTransaction()
                    .add(processing, "processing")
                    .show(processing).commitAllowingStateLoss();

        } catch (JSONException e) {
            NotificationsLoader.makeToast("Unexpected error", true);
            //e.printStackTrace();
        }
    }

    private void password() {
        final CredentialsEditor editor = new CredentialsEditor();
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editor.setDesc(R.string.credentialsPasswordDesc);
        editor.setHint(R.string.accountPasswordNew);
        editor.setField("password");
        editor.setPwdNeeded(true);

        editor.setVerifier(new CredentialsEditor.Verifier() {
            @Override
            public void onSend(@NonNull String value) {
                int complexity = 0;

                if (value.length() < 8) {
                    NotificationsLoader.makeToast("Too short password", true);
                    return;
                }

                for (int i = 0; i < value.length(); ++i) {
                    if (!alphabet.contains(value.charAt(i))) {
                        ++complexity;
                    }
                }

                if (complexity < 3) {
                    NotificationsLoader.makeToast("Too weak password, add more special special characters or numbers", true);
                    return;
                }

                editor.send();
            }
        });

        FragmentsLoader.addChild(editor, this);
    }

    private void login() {
        final CredentialsEditor editor = new CredentialsEditor();
        editor.setInputType(InputType.TYPE_CLASS_TEXT);
        editor.setDesc(R.string.credentialsLoginDesc);
        editor.setHint(R.string.accountLoginNew);
        editor.setField("login");
        editor.setPwdNeeded(true);

        editor.setVerifier(new CredentialsEditor.Verifier() {
            @Override
            public void onSend(@NonNull String value) {
                if (value.length() < 3) {
                    NotificationsLoader.makeToast("Too short login", true);
                    return;
                }

                for (char i: value.toLowerCase().toCharArray()) {
                    if (!alphabet.contains(i)) {
                        NotificationsLoader.makeToast("Login mustn't contain special characters", true);
                        break;
                    }
                }

                editor.send();
            }
        });

        FragmentsLoader.addChild(editor, this);
    }

    private void altAuth() {
        boolean state = !allowAltAuth.isChecked();
        allowAltAuth.setChecked(state);
        DataLoader.set("AllowAltAuth", state);
    }

    private void email() {
        final CredentialsEditor editor = new CredentialsEditor();
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        editor.setDesc(R.string.credentialsEmailDesc);
        editor.setHint(R.string.accountEmailNew);
        editor.setField("email");
        editor.setPwdNeeded(true);

        editor.setVerifier(new CredentialsEditor.Verifier() {
            @Override
            public void onSend(@NonNull String value) {
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                    NotificationsLoader.makeToast("Invalid email", true);
                    return;
                }

                editor.send();
            }
        });

        FragmentsLoader.addChild(editor, this);
    }

    private void closeSessions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        builder.setTitle(res.getString(R.string.closeSessions));
        builder.setMessage(res.getString(R.string.closeSessionsMessage));

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.closeSessionsContinue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Sync.addSyncProvider(
                            new UserLoader.CloseSessions(updater)
                    );

                    processing.setTitle(
                            DataLoader.getAppResources().getString(
                                    R.string.closingSessions
                            )
                    );

                    getChildFragmentManager().beginTransaction()
                            .add(processing, "processing")
                            .show(processing).commitAllowingStateLoss();

                } catch (JSONException e) {
                    NotificationsLoader.makeToast("Unexpected error", true);
                    //e.printStackTrace();
                }
            }
        });

        builder.create().show();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            MainActivity.hideKeyboard();
        }
    }

    @Override
    public void onResult(Bundle data) {
        updater.sendEmptyMessage(-1);
        UserLoader.setSettingsHandler(updater);
    }

    @Override
    public void onDestroy() {
        Sync.removeSyncProvider(Sync.PROVIDER_KEY_CHANGER);
        Sync.removeSyncProvider(Sync.PROVIDER_TERMINATE_SESSIONS);
        super.onDestroy();
    }

    private static class Updater extends Handler {
        private WeakReference<Privacy> weakPrivacy;

        public Updater(@NonNull Privacy privacy) {
            weakPrivacy = new WeakReference<>(privacy);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case -1:
                    reload();
                    break;
                case 0:
                    end(
                        msg.getData().getInt(
                                "status",
                                R.string.unexpectedError
                        )
                    );
                    break;
                case 1:
                    keyChanged();
                    break;
                case 2:
                    terminated();
                    break;
            }
        }

        private void reload() {
            Privacy privacy = weakPrivacy.get();
            if (privacy == null) return;
            View view = privacy.view;

            if (view != null) {
                RadioGroup radioGroup = view.findViewById(R.id.accountKeyLength);
                radioGroup.setOnCheckedChangeListener(null);

                radioGroup.check(
                        DataLoader.getInt("AESBytes", 16) == 16 ? R.id.accountKey128 :
                                R.id.accountKey256
                );

                RadioGroup.OnCheckedChangeListener d = privacy.d;

                privacy.allowAltAuth.setChecked(
                        DataLoader.getBoolean("AllowAltAuth", true)
                );

                privacy.accountAESLength.setText(
                        String.format(
                                Locale.UK,
                                "%s %d bits",
                                DataLoader.getAppResources().getString(R.string.accountEncryptionAESLength),
                                CryptoLoader.getInstalledAESKeyLength() * 8
                        )
                );

                if (d != null) {
                    radioGroup.setOnCheckedChangeListener(d);
                }
            }
        }

        private void end(@StringRes int id) {
            if (id != 0) {
                NotificationsLoader.makeToast(
                        DataLoader.getAppResources().getString(id),
                        true
                );
            }

            Privacy privacy = weakPrivacy.get();

            if (privacy != null && privacy.processing != null) {
                if (!privacy.processing.isInactive()) {
                    privacy.processing.dismiss();
                }
            }
        }

        private void keyChanged() {
            Log.d("LOGTAG", "current ley length: " + CryptoLoader.getInstalledAESKeyLength());
            Sync.removeSyncProvider(Sync.PROVIDER_KEY_CHANGER);
            Privacy privacy = weakPrivacy.get();

            if (privacy != null) {
                privacy.startedChangingAESKey = false;
            }

            end(R.string.success);
        }

        private void terminated() {
            Sync.removeSyncProvider(Sync.PROVIDER_TERMINATE_SESSIONS);
            end(R.string.success);
        }
    }
}
