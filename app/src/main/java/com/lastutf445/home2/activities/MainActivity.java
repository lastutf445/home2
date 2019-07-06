package com.lastutf445.home2.activities;

import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
import java.util.ArrayDeque;

public class MainActivity extends AppCompatActivity {

    private static WeakReference<MainActivity> instance;
    private ArrayDeque<Integer> stack;
    private NavigationFragment active;
    private BottomNavigationView nav;
    private Handler handler;

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
    protected void onPostResume() {
        super.onPostResume();
        handler.postDelayed(new WakeUp(), 800);
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

            if (stack.size() > 1) {
                stack.pollLast();
                nav.setSelectedItemId(stack.getLast());

            } else {
                super.onBackPressed();
            }
        }
    }

    private void init() {
        Thread.currentThread().setPriority(8);

        stack = new ArrayDeque<>();
        handler = new Handler();

        CryptoLoader.init();
        ModulesLoader.init();
        UserLoader.init();

        FragmentsLoader.init(
                getSupportFragmentManager(),
                R.id.content
        );

        DataLoader.setWithoutSync("FirstStart", false);
        DataLoader.save();

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

        nav = findViewById(R.id.nav);
        NotificationsLoader.init(nav);
        Sync.init();

        BottomNavigationView.OnNavigationItemSelectedListener c = new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == nav.getSelectedItemId()) {
                    return false;
                }

                NavigationFragment root;
                int id;

                switch (item.getItemId()) {
                    case R.id.nav_dashboard:
                        id = R.id.nav_dashboard;
                        root = dashboard;
                        break;
                    case R.id.nav_messages:
                        id = R.id.nav_messages;
                        root = messages;
                        break;
                    case R.id.nav_notifications:
                        id = R.id.nav_notifications;
                        root = notifications;
                        break;
                    default:
                        id = R.id.nav_menu;
                        root = menu;
                        break;
                }

                active = root;
                stack.remove(id);
                stack.addLast(id);

                FragmentsLoader.changeFragment(
                        FragmentsLoader.getTop(root),
                        FragmentsLoader.getPrimaryNavigationFragment(),
                        false, false
                );

                return true;
            }
        };

        nav.setSelectedItemId(R.id.nav_dashboard);
        nav.setOnNavigationItemSelectedListener(c);
        FragmentsLoader.changeFragment(dashboard, null, false, false);
        stack.addLast(R.id.nav_dashboard);
    }

    public static MainActivity getInstance() {
        return instance != null ? instance.get() : null;
    }

    public static void hideKeyboard() {
        AppCompatActivity activity = instance.get();

        if (activity == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();

        if (view == null) {
            view = new View(activity);
        }

        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public Handler getHandler() {
        // helps me for debugging and testing
        return handler;
    }

    private static class WakeUp implements Runnable {
        @Override
        public void run() {
            Sync.updateNetworkState2();
        }
    }
}
