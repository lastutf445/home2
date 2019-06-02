package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Privacy extends NavigationFragment {

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
        updater = new Updater(view);
        radioGroup = view.findViewById(R.id.accountKeyLength);

        radioGroup.check(
                DataLoader.getInt("AESBytes", 16) == 16 ? R.id.accountKey128 :
                        R.id.accountKey256
        );

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

        radioGroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
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
                }
        );

        view.findViewById(R.id.accountGenAES).setOnClickListener(c);
    }

    private void generateAES() {

    }

    @Override
    public void onResult(Bundle data) {
        updater.sendEmptyMessage(-1);
        UserLoader.setSettingsHandler(updater);
    }

    private static class Updater extends Handler {
        private WeakReference<View> weakView;

        public Updater(View view) {
            weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == -1) reload();
        }

        private void reload() {
            View view = weakView.get();

            if (view != null) {

                ((RadioGroup) view.findViewById(R.id.accountKeyLength)).check(
                        DataLoader.getInt("AESBytes", 16) == 16 ? R.id.accountKey128 :
                                R.id.accountKey256
                );
            }
        }
    }
}
