package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.util.NavigationFragment;

public class AuthKey extends NavigationFragment {

    private EditText field;
    private boolean recorded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.authkey, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.authKeyApply:
                        apply();
                        break;
                    case R.id.authKeyCancel:
                        cancel();
                        break;
                }
            }
        };

        view.findViewById(R.id.authKeyApply).setOnClickListener(c);
        view.findViewById(R.id.authKeyCancel).setOnClickListener(c);
        field = view.findViewById(R.id.authKeyField);
    }

    private void apply() {
        String t = field.getText().toString().trim();

        if (t.length() == 0) {
            NotificationsLoader.makeToast("Auth key mustn\'t be empty", true);
            return;
        }

        recorded = true;
        toParent.putString("authKey", t);
        getActivity().onBackPressed();
    }

    private void cancel() {
        if (!recorded) {
            recorded = true;
            toParent.putString("authKey", "");
            getActivity().onBackPressed();
        }

    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        MainActivity.hideKeyboard();
        super.onHiddenChanged(hidden);
    }

    @Override
    public boolean onBackPressed() {
        if (recorded) return true;
        else {
            cancel();
            return false;
        }
    }
}
