package com.lastutf445.home2.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.fragments.menu.About;
import com.lastutf445.home2.fragments.menu.Account;
import com.lastutf445.home2.fragments.menu.Auth;
import com.lastutf445.home2.fragments.menu.Dashboard;
import com.lastutf445.home2.fragments.menu.Messages;
import com.lastutf445.home2.fragments.menu.Modules;
import com.lastutf445.home2.fragments.menu.Nodes;
import com.lastutf445.home2.fragments.menu.Notifications;
import com.lastutf445.home2.fragments.menu.Sync;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.util.NavigationFragment;

public final class Menu extends NavigationFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_menu, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavigationFragment child = null;

                switch (v.getId()) {
                    case R.id.menuAccount:
                        child = (UserLoader.isAuthenticated() ? new Account() : new Auth());
                        break;
                    case R.id.menuDashboard:
                        child = new Dashboard();
                        break;
                    case R.id.menuMessages:
                        child = new Messages();
                        break;
                    case R.id.menuNotifications:
                        child = new Notifications();
                        break;
                    case R.id.menuModules:
                        Modules modules = new Modules();
                        modules.setModules(ModulesLoader.getModules(), false);
                        child = modules;
                        break;
                    case R.id.menuNodes:
                        child = new Nodes();
                        break;
                    case R.id.menuSync:
                        child = new Sync();
                        break;
                    case R.id.menuAbout:
                        child = new About();
                        break;
                    default:
                        NotificationsLoader.makeToast(
                                DataLoader.getAppResources().getString(R.string.unknownError),
                                true
                        );
                }

                if (child != null) {
                    FragmentsLoader.addChild(child, Menu.this);
                }
            }
        };

        int[] buttons = {
                R.id.menuAccount,
                R.id.menuDashboard,
                R.id.menuMessages,
                R.id.menuNotifications,
                R.id.menuModules,
                R.id.menuNodes,
                R.id.menuSync,
                R.id.menuAbout
        };

        for (int i: buttons) {
            view.findViewById(i).setOnClickListener(c);
        }

        reload();
    }

    @Override
    protected void reload() {
        ((TextView) view.findViewById(R.id.menuAccount)).setText(
                UserLoader.getUsername()
        );
    }

    /**
     * BUNDLE KEYS:
     * reload - reload parental fragment
     * deleted - deleted item on pos (integer value)
     * updated - updated item on pos (integer value)
     * added - added item on pos (integer value)
     *
     */

    @Override
    public void onResult(Bundle data) {
        if (data.getBoolean("reload")) {
            reload();
        }
    }
}
