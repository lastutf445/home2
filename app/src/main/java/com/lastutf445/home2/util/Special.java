package com.lastutf445.home2.util;

import android.support.annotation.NonNull;

import com.lastutf445.home2.containers.Module;

public abstract class Special extends NavigationFragment {

    private Module module;

    public void setModule(@NonNull Module module) {
        this.module = module;
    }
}
