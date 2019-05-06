package com.lastutf445.home2.loaders;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

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

    public static void addFragment(NavigationFragment fragment) {
        manager.beginTransaction().add(contentId, fragment).hide(fragment).commitNow();
        fragment.setParent(fragment);
    }

    public static void addChild(NavigationFragment fragment, NavigationFragment parent) {
        addFragment(fragment);
        parent.setChild(fragment);
        fragment.setParent(parent);
        changeFragment(fragment, false, true);
    }

    public static void changeFragment(NavigationFragment fragment, boolean animRemove, boolean animAdd) {
        FragmentTransaction ft = manager.beginTransaction();

        if (manager.getPrimaryNavigationFragment() != null) {
            if (animRemove) {
                ft.setCustomAnimations(R.anim.fragment_remove, R.anim.fragment_remove);
            }
            else if (animAdd) {
                ft.setCustomAnimations(R.anim.fragment_hide, R.anim.fragment_hide);
            }

            ft.hide(manager.getPrimaryNavigationFragment()).commitNow();
        }


        if (animAdd) {
            ft.setCustomAnimations(R.anim.fragment_add, R.anim.fragment_add);
        }
        else if (animRemove) {
            ft.setCustomAnimations(R.anim.fragment_restore, R.anim.fragment_restore);
        }

        ft.show(fragment).commitNow();
        ft.setPrimaryNavigationFragment(fragment).commitNow();
    }

    private static void removeFragment(NavigationFragment fragment) {
        manager.beginTransaction().setCustomAnimations(R.anim.fragment_remove, R.anim.fragment_remove).remove(fragment).commitNow();
        fragment.getParent().onResult(fragment.getResult());
        fragment.getParent().setChild(null);
    }

    public static NavigationFragment getTop(NavigationFragment base) {
        while (base.getChild() != null) base = base.getChild();
        return base;
    }

    public static boolean pop(NavigationFragment base) {
        NavigationFragment top = getTop(base);
        NavigationFragment parent = top.getParent();

        if (parent != top) {
            removeFragment(top);
            changeFragment(parent, true, false);
        }

        return parent != top;
    }

    public static void clear() {
        FragmentTransaction t = manager.beginTransaction();

        for (Fragment i: manager.getFragments()) {
            t.remove(i);
        }

        t.commitNow();
    }
}
