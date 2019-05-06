package com.lastutf445.home2.special;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.Special;

public class TemperatureSpecial extends Special {

    private Module module;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.special_temperature, container, false);
        init();
        return view;
    }

    public void setModule(@NonNull Module module) {
        this.module = module;
    }
}
