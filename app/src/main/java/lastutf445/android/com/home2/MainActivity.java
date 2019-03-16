package lastutf445.android.com.home2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private Dashboard dashboard = new Dashboard(); // listenerId: from -3 to -inf
    private Messages messages = new Messages(); // listenerId: -2
    private Notifications notifications = new Notifications(); // listenerId: -1
    private Menu menu = new Menu();

    private FragmentManager manager;
    private static NavigationFragment active;
    private static Context appContext;
    private static Resources appResources;

    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = getApplicationContext();
        appResources = getResources();

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

        initFragmentSystem();

        nav.setOnNavigationItemSelectedListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Database.kill();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Notifications.makeToast( manager.getFragments().size() + " fragments are injected");
    }

    private void initFragmentSystem() {
        addFragment(dashboard);
        addFragment(messages);
        addFragment(notifications);
        addFragment(menu);

        switch (Data.getString("ActiveFragment", "dashboard")) {
            case "dashboard":
                changeFragment(dashboard);
                break;
            case "messages":
                changeFragment(messages);
                break;
            case "notifications":
                changeFragment(notifications);
                break;
            case "menu":
                changeFragment(menu);
                break;
        }
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
        if (active != null) manager.beginTransaction().hide(active).commit();
        manager.beginTransaction().show(fragment).commit();
        active = fragment;

        Log.d("LOGTAG", fragment.getClass().getName() + " is now shown");
    }

    public static void universalViewerSetupBridge(UniversalViewer uv, View view, int layout) {
        active.universalViewerSetup(uv, view, layout);
    }

    public static Context getAppContext() {
        return appContext;
    }

    public static Resources getAppResources() {
        return appResources;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Data.refreshOptions();

        switch(requestCode) {
            case 0:
                active.onActivityResult(resultCode, data);
                break;
        }
    }
}
