package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class Privacy extends NavigationFragment {

    private RadioGroup.OnCheckedChangeListener d;
    private ArrayList<Character> alphabet;
    private RadioGroup radioGroup;
    private Updater updater;

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

        updater = new Updater(view, d);
        radioGroup = view.findViewById(R.id.accountKeyLength);
        UserLoader.setSettingsHandler(updater);

        radioGroup.check(
                DataLoader.getInt("AESBytes", 16) == 16 ? R.id.accountKey128 :
                        R.id.accountKey256
        );

        view.findViewById(R.id.accountGenAES).setOnClickListener(c);
        view.findViewById(R.id.accountPassword).setOnClickListener(c);
        view.findViewById(R.id.accountLogin).setOnClickListener(c);
        radioGroup.setOnCheckedChangeListener(d);
    }

    private void generateAES() {

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

    private static class Updater extends Handler {
        private WeakReference<RadioGroup.OnCheckedChangeListener> weakD;
        private WeakReference<View> weakView;

        public Updater(View view, RadioGroup.OnCheckedChangeListener d) {
            weakView = new WeakReference<>(view);
            weakD = new WeakReference<>(d);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == -1) reload();
        }

        private void reload() {
            View view = weakView.get();

            if (view != null) {
                RadioGroup radioGroup = view.findViewById(R.id.accountKeyLength);
                radioGroup.setOnCheckedChangeListener(null);

                radioGroup.check(
                        DataLoader.getInt("AESBytes", 16) == 16 ? R.id.accountKey128 :
                                R.id.accountKey256
                );

                RadioGroup.OnCheckedChangeListener d = weakD.get();

                if (d != null) {
                    radioGroup.setOnCheckedChangeListener(d);
                }
            }
        }
    }
}
