package com.lastutf445.home2.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.lastutf445.home2.R;
import com.lastutf445.home2.fragments.Dashboard;
import com.lastutf445.home2.fragments.Menu;
import com.lastutf445.home2.fragments.Messages;
import com.lastutf445.home2.fragments.Notifications;
import com.lastutf445.home2.loaders.CryptoLoader;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static WeakReference<MainActivity> instance;
    private NavigationFragment active;

    private Dashboard dashboard;
    private Messages messages;
    private Notifications notifications;
    private Menu menu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onResume() {
        Sync.updateNetworkState2();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        try {
            DataLoader.kill();
            Sync.stop();
        } catch (Exception e) {
            // lol
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!FragmentsLoader.pop(active)) {
            super.onBackPressed();
        }
    }

    private void init() {
        Thread.currentThread().setPriority(8);

        DataLoader.init(
                getApplicationContext(),
                getResources()
        );

        CryptoLoader.init();
        ModulesLoader.init();
        UserLoader.init();

        FragmentsLoader.init(
                getSupportFragmentManager(),
                R.id.content
        );

        dashboard = new Dashboard();
        messages = new Messages();
        notifications = new Notifications();
        menu = new Menu();

        instance = new WeakReference<>(this);
        active = dashboard;

        FragmentsLoader.clear();
        FragmentsLoader.addFragment(dashboard);
        FragmentsLoader.addFragment(messages);
        FragmentsLoader.addFragment(notifications);
        FragmentsLoader.addFragment(menu);

        final BottomNavigationView nav = findViewById(R.id.nav);
        NotificationsLoader.init(nav);
        Sync.init();

        BottomNavigationView.OnNavigationItemSelectedListener c = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == nav.getSelectedItemId()) {
                    return false;
                }

                NavigationFragment root;

                switch (item.getItemId()) {
                    case R.id.nav_dashboard:
                        root = dashboard;
                        break;
                    case R.id.nav_messages:
                        root = messages;
                        break;
                    case R.id.nav_notifications:
                        root = notifications;
                        break;
                    default:
                        root = menu;
                        break;
                }

                active = root;

                FragmentsLoader.changeFragment(
                        FragmentsLoader.getTop(root)
                );

                return true;
            }
        };

        nav.setSelectedItemId(R.id.nav_dashboard);
        nav.setOnNavigationItemSelectedListener(c);
        FragmentsLoader.changeFragment(dashboard, false, false);
    }

    public static MainActivity getInstance() {
        return instance != null ? instance.get() : null;
    }
}
