package com.lastutf445.home2.fragments.scenarios;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.util.NavigationFragment;

public class ScenarioOpsEditor extends NavigationFragment {

    private Module module;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.scenario_ops_editor, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {

    }

    public void setModule(@NonNull Module module) {
        this.module = module;
    }
}
