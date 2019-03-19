package lastutf445.android.com.home2;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private Dashboard dashboard = new Dashboard(); // listenerId: 0+ (nodeSerial)
    private Messages messages = new Messages(); // listenerId: -1
    private Notifications notifications = new Notifications(); // listenerId: -2
    private Menu menu = new Menu();

    private static FragmentManager manager;
    private static NavigationFragment active;
    private static Context appContext;
    private static Resources appResources;

    private static Handler UVHandler;
    private static MainHandler handler;

    private static class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            try {
                Bundle data = msg.getData();

                 switch(msg.what) {
                     case 0:
                         Dashboard.onUpdate(data);
                         break;
                     case 999:
                         Notifications.makeToast(data.getString("msg"));
                         break;
                 }

            } catch (NullPointerException e) {
                e.printStackTrace();
            }

        }
    }

    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = getApplicationContext();
        appResources = getResources();
        handler = new MainHandler();

        manager = getSupportFragmentManager();
        nav = findViewById(R.id.nav);

        Database.init();
        Data.refreshOptions();
        Modules.refreshNodes();
        Modules.refreshModules();

        Sync.init();
        boolean sync = Data.getBoolean("Sync", false);
        Dashboard.setSync(Data.getBoolean("SyncDashboard", false) && sync);
        //Messages.setSync(Data.getBoolean("SyncMessages", false) && sync);
        //Notifications.setSync(Data.getBoolean("SyncNotifications", false) && sync);

        initFragmentSystem(
                savedInstanceState == null ? null :
                savedInstanceState.getString("activeFragment", null)
        );

        nav.setOnNavigationItemSelectedListener(this);
        Notifications.makeToast( manager.getFragments().size() + " fragments are injected");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("activeFragment", active.getFragmentId());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Database.kill();
    }

    private void initFragmentSystem(String activeFragment) {
        clearFragments();

        addFragment(dashboard);
        addFragment(messages);
        addFragment(notifications);
        addFragment(menu);

        if (activeFragment == null) {
            activeFragment = Data.getString("ActiveFragment", "dashboard");
        }

        switch (activeFragment) {
            case "dashboard":
                changeFragment(dashboard);
                nav.setSelectedItemId(nav.getMenu().getItem(0).getItemId());
                break;
            case "messages":
                changeFragment(messages);
                nav.setSelectedItemId(nav.getMenu().getItem(1).getItemId());
                break;
            case "notifications":
                changeFragment(notifications);
                nav.setSelectedItemId(nav.getMenu().getItem(2).getItemId());
                break;
            case "menu":
                changeFragment(menu);
                nav.setSelectedItemId(nav.getMenu().getItem(3).getItemId());
                break;
        }
    }

    private void clearFragments() {
        FragmentTransaction t = manager.beginTransaction();

        for (Fragment i: manager.getFragments()) {
            t.remove(i);
        }

        t.commitNow();
    }

    private void addFragment(NavigationFragment fragment) {
        manager.beginTransaction().add(R.id.content, fragment).hide(fragment).commitNow();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == nav.getSelectedItemId()) {
            return false;
        }

        switch(item.getItemId()) {
            case R.id.nav_dashboard:
                changeFragment(dashboard);
                break;
            case R.id.nav_messages:
                changeFragment(messages);
                break;
            case R.id.nav_notifications:
                changeFragment(notifications);
                break;
            case R.id.nav_menu:
                changeFragment(menu);
                break;
            default:
                return false;
        }

        return true;
    }

    private void changeFragment(NavigationFragment fragment) {
        if (active != null) manager.beginTransaction().hide(active).commitNow();
        manager.beginTransaction().show(fragment).commitNow();
        active = fragment;

        Log.d("LOGTAG", fragment.getClass().getName() + " is now shown");
    }

    public static void universalViewerSetupBridge(UniversalViewer uv, View view, int layout) {
        active.universalViewerSetup(uv, view, layout);
    }

    public static Fragment getActiveFragment() {
        return active;
    }

    public static Context getAppContext() {
        return appContext;
    }

    public static Resources getAppResources() {
        return appResources;
    }

    public synchronized static Handler getMainHandler() {
        return handler;
    }

    public synchronized static void setUVHandler(Handler handler) {
        UVHandler = handler;
    }

    public synchronized static Handler getUVHandler() {
        return UVHandler;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Data.refreshOptions();

        //Log.d("LOGTAG", "captured activity result - " + requestCode + ", " + resultCode);

        switch(requestCode) {
            case 0:
                active.onActivityResult(resultCode, data);
                break;
        }
    }
}
