package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Privacy extends NavigationFragment {

    private RadioGroup.OnCheckedChangeListener d;
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

        updater = new Updater(view, d);
        radioGroup = view.findViewById(R.id.accountKeyLength);
        UserLoader.setSettingsHandler(updater);

        radioGroup.check(
                DataLoader.getInt("AESBytes", 16) == 16 ? R.id.accountKey128 :
                        R.id.accountKey256
        );

        view.findViewById(R.id.accountGenAES).setOnClickListener(c);
        radioGroup.setOnCheckedChangeListener(d);
    }

    private void generateAES() {

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
