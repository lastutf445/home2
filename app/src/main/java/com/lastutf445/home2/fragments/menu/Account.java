package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.util.NavigationFragment;

public class Account extends NavigationFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_account, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        reload();

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
    }

    @Override
    protected void reload() {
        TextView username = view.findViewById(R.id.accountUsername);
        username.setText(UserLoader.getUsername());
    }

    private void rename() {
        Rename rename = new Rename();

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

                } else {
                    NotificationsLoader.makeToast("Success", true);
                    DataLoader.set("Username", t);
                    DataLoader.save();
                    return true;
                }
            }
        });

        FragmentsLoader.addChild(rename, Account.this);
    }

    private void logout() {
        UserLoader.logout();

        NotificationsLoader.makeToast(
                DataLoader.getAppResources().getString(R.string.authSuccess),
                true
        );

        toParent.putBoolean("reload", true);
        getActivity().onBackPressed();

    }

    @Override
    public void onResult(Bundle data) {
        if (data.getBoolean("reload")) {
            toParent.putBoolean("reload", true);
            reload();
        }
    }
}
