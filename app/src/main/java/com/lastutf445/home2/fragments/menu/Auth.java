package com.lastutf445.home2.fragments.menu;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.util.NavigationFragment;

public class Auth extends NavigationFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_auth, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
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

        int status = UserLoader.auth(login, password);
        Resources res = DataLoader.getAppResources();
        int msgId = R.string.unknownError;

        switch (status) {
            case 0:
                msgId = R.string.syncDisconnected;
                break;
            case 1:
                msgId = R.string.authSuccess;
                break;
            case 2:
                msgId = R.string.authIncorrect;
                break;
            case 3:
                msgId = R.string.authUnableToConnect;
                break;
        }

        NotificationsLoader.makeToast(
                res.getString(msgId),
                true
        );

        if (msgId == R.string.authSuccess) {
            toParent.putBoolean("reload", true);
            getActivity().onBackPressed();
        }
    }

    private void enterBasic() {
        UserLoader.authBasic();

        NotificationsLoader.makeToast(
                DataLoader.getAppResources().getString(R.string.authSuccess),
                true
        );

        toParent.putBoolean("reload", true);
        getActivity().onBackPressed();
    }
}
