package com.lastutf445.home2.loaders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.lastutf445.home2.R;
import com.lastutf445.home2.util.NavigationFragment;

public final class FragmentsLoader {

    private static FragmentManager manager;
    private static int contentId;

    public static void init(FragmentManager manager, int contentId) {
        FragmentsLoader.manager = manager;
        FragmentsLoader.contentId = contentId;
        clear();
    }

    public static void addFragment(@NonNull NavigationFragment fragment) {
        manager.beginTransaction().add(contentId, fragment).hide(fragment).commitNow();
        fragment.setParent(fragment);
    }

    public static void addChild(@NonNull NavigationFragment fragment, NavigationFragment parent) {
        addFragment(fragment);
        parent.setChild(fragment);
        fragment.setParent(parent);
        changeFragment(fragment, false, true);
    }

    public static void changeFragment(@NonNull NavigationFragment fragment, boolean animRemove, boolean animAdd) {
        FragmentTransaction ft = manager.beginTransaction();

        if (manager.getPrimaryNavigationFragment() != null) {
            if (animRemove) {
                ft.setCustomAnimations(R.anim.fragment_remove, R.anim.fragment_remove);
            }
            else if (animAdd) {
                ft.setCustomAnimations(R.anim.fragment_hide, R.anim.fragment_hide);
            }

            ft.hide(manager.getPrimaryNavigationFragment()).commitAllowingStateLoss();
            ft = manager.beginTransaction();
        }

        if (animAdd) {
            ft.setCustomAnimations(R.anim.fragment_add, R.anim.fragment_add);
        }
        else if (animRemove) {
            ft.setCustomAnimations(R.anim.fragment_restore, R.anim.fragment_restore);
        }

        ft.show(fragment);
        ft.setPrimaryNavigationFragment(fragment).commitAllowingStateLoss();
    }

    public static void changeFragment(@NonNull NavigationFragment fragment) {
        FragmentTransaction ft = manager.beginTransaction();

        if (manager.getPrimaryNavigationFragment() != null) {
            ft.setCustomAnimations(R.anim.fragment_erase, R.anim.fragment_erase);
            ft.hide(manager.getPrimaryNavigationFragment()).commitAllowingStateLoss();
            ft = manager.beginTransaction();
        }

        ft.show(fragment);
        ft.setPrimaryNavigationFragment(fragment).commitAllowingStateLoss();
    }

    @Nullable
    public static NavigationFragment getPrimaryNavigationFragment() {
        return (NavigationFragment) manager.getPrimaryNavigationFragment();
    }

    private static void removeFragment(@NonNull NavigationFragment fragment) {
        manager.beginTransaction().setCustomAnimations(R.anim.fragment_remove, R.anim.fragment_remove).remove(fragment).commitAllowingStateLoss();
        fragment.getParent().onResult(fragment.getResult());
        fragment.getParent().setChild(null);
        fragment.setParent(null);
        fragment.setChild(null);
    }

    @NonNull
    public static NavigationFragment getTop(NavigationFragment base) {
        while (base.getChild() != null) base = base.getChild();
        return base;
    }

    public static boolean pop(@NonNull NavigationFragment base) {
        NavigationFragment top = getTop(base);
        NavigationFragment parent = top.getParent();

        if (parent != top) {
            removeFragment(top);
            changeFragment(parent, true, false);
        }

        return parent != top;
    }

    public static void pop2(@NonNull NavigationFragment base) {
        NavigationFragment top = getTop(base);
        NavigationFragment parent = top.getParent();

        if (parent != top) {
            removeFragment(top);
        }
    }

    public static void clear() {
        FragmentTransaction t = manager.beginTransaction();

        for (Fragment i: manager.getFragments()) {
            t.remove(i);
        }

        t.commitNow();
    }
}
