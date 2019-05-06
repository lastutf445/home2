package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.util.NavigationFragment;

public class Rename extends NavigationFragment {

    private EditText renameNew = null;
    private Callback callback = null;
    private String title = null;
    private String hint = null;
    private String old = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_rename, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        view.findViewById(R.id.renameApply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    if (callback.onApply(renameNew.getText().toString())) {
                        toParent.putBoolean("reload", true);
                        getActivity().onBackPressed();
                    }
                }
            }
        });

        ((TextView) view.findViewById(R.id.renameTitle)).setText(title);
        ((TextView) view.findViewById(R.id.renameOld)).setText(old);
        ((EditText) view.findViewById(R.id.renameNew)).setHint(hint);
        renameNew = view.findViewById(R.id.renameNew);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setOld(String old) {
        this.old = old;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        boolean onApply(String s);
    }
}
