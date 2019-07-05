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
        manager.beginTransaction().add(contentId, fragment).hide(fragment).commitAllowingStateLoss();
        fragment.setParent(fragment);
    }

    public synchronized static void addChild(@NonNull NavigationFragment fragment, NavigationFragment parent) {
        addFragment(fragment);
        parent.setChild(fragment);
        fragment.setParent(parent);
        changeFragment(fragment, parent, false, true);
    }

    public synchronized static void changeFragment(@NonNull NavigationFragment fragment, @Nullable NavigationFragment previous, boolean animRemove, boolean animAdd) {
        FragmentTransaction ft = manager.beginTransaction();

        if (previous != null) {
            if (animRemove) {
                ft.setCustomAnimations(R.anim.fragment_remove, R.anim.fragment_remove);

            } else if (animAdd) {
                ft.setCustomAnimations(R.anim.fragment_hide, R.anim.fragment_hide);
            }

            ft.hide(previous).commitAllowingStateLoss();
            ft = manager.beginTransaction();
        }

        if (animAdd) {
            ft.setCustomAnimations(R.anim.fragment_add, R.anim.fragment_add);

        } else if (animRemove) {
            ft.setCustomAnimations(R.anim.fragment_restore, R.anim.fragment_restore);
        }

        ft.show(fragment);

        if (!fragment.isRemoving() && !fragment.isDetached()) {
            ft.setPrimaryNavigationFragment(fragment).commitAllowingStateLoss();
        }
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

    public synchronized static void removeFragment2(@NonNull NavigationFragment fragment) {
        manager.beginTransaction().remove(fragment).commitAllowingStateLoss();
        fragment.setParent(null);
        fragment.setChild(null);
    }

    @NonNull
    public static NavigationFragment getTop(NavigationFragment base) {
        while (base.getChild() != null) base = base.getChild();
        return base;
    }

    public synchronized static boolean pop(@NonNull NavigationFragment base) {
        NavigationFragment top = getTop(base);
        NavigationFragment parent = top.getParent();

        if (parent != top) {
            if (!top.onBackPressed()) {
                return true;
            }

            removeFragment(top);
            changeFragment(parent, top, true, false);
            parent.onPostResult(top.getResult());
        }

        return parent != top;
    }

    public synchronized static void pop2(@NonNull NavigationFragment base) {
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

        t.commitAllowingStateLoss();
    }
}
