package com.lastutf445.home2.util;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lastutf445.home2.R;

public class NavigationFragment extends Fragment {

    protected NavigationFragment parent, child;
    @NonNull
    protected Bundle toParent = new Bundle();
    protected View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_locked, container, false);
        return view;
    }

    protected void init() {}

    protected void reload() {}

    @NonNull
    public Bundle getResult() {
        return toParent;
    }

    public void onResult(Bundle data) {}

    public void setChild(NavigationFragment child) {
        this.child = child;
    }

    public NavigationFragment getChild() {
        return child;
    }

    public void setParent(NavigationFragment parent) {
        this.parent = parent;
    }

    public NavigationFragment getParent() {
        return parent;
    }
}
