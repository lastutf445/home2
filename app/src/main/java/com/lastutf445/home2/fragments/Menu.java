package com.lastutf445.home2.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.fragments.menu.About;
import com.lastutf445.home2.fragments.menu.Account;
import com.lastutf445.home2.fragments.menu.Auth;
import com.lastutf445.home2.fragments.menu.Dashboard;
import com.lastutf445.home2.fragments.menu.Messages;
import com.lastutf445.home2.fragments.menu.Modules;
import com.lastutf445.home2.fragments.menu.Notifications;
import com.lastutf445.home2.fragments.menu.Sync;
import com.lastutf445.home2.fragments.scenarios.Scenarios;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;

import java.lang.ref.WeakReference;

public final class Menu extends NavigationFragment {

    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_menu, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(view);
        UserLoader.setSettingsHandler(updater);

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
                    case R.id.menuScenarios:
                        child = new Scenarios();
                        break;
                    case R.id.menuModules:
                        Modules modules = new Modules();
                        modules.setModules(ModulesLoader.getModules(), false);
                        modules.enableAddButton();
                        modules.setRemovable(true);
                        child = modules;
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
                R.id.menuScenarios,
                R.id.menuModules,
                R.id.menuSync,
                R.id.menuAbout
        };

        for (int i: buttons) {
            view.findViewById(i).setOnClickListener(c);

            SimpleAnimator.drawableTint(
                    (Button) view.findViewById(i),
                    Color.parseColor(
                            i == R.id.menuAccount
                                    ? "#00695C" : "#444444"
                    )
            );
        }

        updater.sendEmptyMessage(-1);
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
        updater.sendEmptyMessage(-1);
        UserLoader.setSettingsHandler(updater);
    }

    private static class Updater extends Handler {
        private WeakReference<View> weakView;

        public Updater(View view) {
            weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == -1) reload();
        }

        private void reload() {
            View view = weakView.get();

            if (view != null) {
                ((TextView) view.findViewById(R.id.menuAccount)).setText(
                        UserLoader.getUsername()
                );
            }
        }
    }
}
