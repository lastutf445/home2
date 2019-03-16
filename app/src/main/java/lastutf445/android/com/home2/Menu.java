package lastutf445.android.com.home2;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Menu extends NavigationFragment implements View.OnClickListener {

    private int[] buttons = {
            R.id.menuAccountButton,
            R.id.menuDashboardButton,
            R.id.menuMessagesButton,
            R.id.menuNotificationsButton,
            R.id.menuStorageButton,
            R.id.menuModulesButton,
            R.id.menuSyncButton,
            R.id.menuAboutButton
    };

    private View view;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_menu, container, false);
        navigationFragmentSetup();
        return view;
    }

    @Override
    public void navigationFragmentSetup() {

        if (API.isAuthorized()) {
            String accountName = Data.getString("AccountName", null);

            if (accountName == null) {
                accountName = getResources().getString(R.string.menu_account_name_error);
            }

            else {
                accountName = getResources().getString(R.string.menu_user_hello) + " " + accountName;
            }

            ((TextView) view.findViewById(R.id.menuAccountButton)).setText(accountName);
        }

        else {
            ((TextView) view.findViewById(R.id.menuAccountButton)).setText(
                    getResources().getText(R.string.menu_user_is_not_authorized)
            );
        }

        for (int i: buttons) {
            view.findViewById(i).setOnClickListener(this);
        }

    }

    @Override
    public void onClick(View v) {
        Intent i = new Intent(getContext(), UniversalViewer.class);

        switch(v.getId()) {
            case R.id.menuAccountButton:
                i.putExtra("layout", API.isAuthorized() ? R.layout.menu_account : R.layout.menu_auth);
                break;
            case R.id.menuDashboardButton:
                i.putExtra("layout", R.layout.menu_dashboard);
                break;
            case R.id.menuMessagesButton:
                i.putExtra("layout", R.layout.menu_messages);
                break;
            case R.id.menuNotificationsButton:
                i.putExtra("layout", R.layout.menu_notifications);
                break;
            case R.id.menuStorageButton:
                i.putExtra("layout", R.layout.menu_storage);
                break;
            case R.id.menuModulesButton:
                i.putExtra("layout", R.layout.menu_modules);
                break;
            case R.id.menuSyncButton:
                i.putExtra("layout", R.layout.menu_sync);
                break;
            case R.id.menuAboutButton:
                i.putExtra("layout", R.layout.menu_about);
                break;
            default:
                Toast.makeText(getContext(), "can you call uncallable?", Toast.LENGTH_LONG).show();
                break;
        }

        if (i.hasExtra("layout")) {
            i.putExtra("returnCode", 0);
            startActivityForResult(i, 0);
        }
    }

    @Override
    public void onActivityResult(int resultCode, Intent data) {
        //super.onActivityResult(resultCode, data);

        Notifications.makeToast("menu: " + resultCode);

    }

    @Override
    public void universalViewerSetup(UniversalViewer uv, View view, int layout) {
        //super.universalViewerSetup(view, layout);

        switch(layout) {
            case R.layout.menu_account:
                setupAccountScreen(uv, view);
                break;
            case R.layout.menu_account_edit_name:
                setupAccountEditName(uv, view);
                break;
            case R.layout.menu_auth:
                setupAuthScreen(uv, view);
                break;
            case R.layout.menu_dashboard:
                setupDashboardScreen(uv, view);
                break;
            case R.layout.menu_messages:
                setupMessagesScreen(uv, view);
                break;
            case R.layout.menu_notifications:
                setupNotificationsScreen(uv, view);
                break;
            case R.layout.menu_storage:
                setupStorageScreen(uv, view);
                break;
            case R.layout.menu_modules:
                setupModulesScreen(uv, view);
                break;
            case R.layout.menu_sync:
                setupSyncScreen(uv, view);
                break;
            case R.layout.menu_sync_masterserver:
                setupSyncMasterServerScreen(uv, view);
                break;
            case R.layout.menu_about:
                setupAboutScreen(uv, view);
                break;
        }
    }

    private void setAccountName(TextView view) {
        String accountName = Data.getString("AccountName", null);

        if (accountName == null) {
            accountName = getResources().getString(R.string.menu_account_name_error);
        }

        view.setText(accountName);
    }

    private void setupAccountScreen(UniversalViewer uv, View view) {
        UniversalViewerListener c = new UniversalViewerListener(uv, view) {
            @Override
            public void onClick(View v) {
                switch(v.getId()) {
                    case R.id.accountEditAccountName:
                        Intent i = new Intent(getContext(), UniversalViewer.class);
                        i.putExtra("layout", R.layout.menu_account_edit_name);
                        i.putExtra("needReload", true);
                        uv.startActivityForResult(i, 0);
                        break;
                    case R.id.accountEditPrivacy:

                        break;
                    case R.id.accountLogout:
                        API.logout();
                        navigationFragmentSetup();
                        uv.kill();
                        break;
                }
            }
        };

        setAccountName((TextView) view.findViewById(R.id.accountName));

        view.findViewById(R.id.accountEditAccountName).setOnClickListener(c);
        view.findViewById(R.id.accountEditPrivacy).setOnClickListener(c);
        view.findViewById(R.id.accountLogout).setOnClickListener(c);

    }

    private void setupAccountEditName(UniversalViewer uv, View view) {
        UniversalViewerListener c = new UniversalViewerListener(uv, view) {
            @Override
            public void onClick(View v) {
                switch(v.getId()) {
                    case R.id.accountEditNameApply:
                        API.rename(((TextView) view.findViewById(R.id.accountEditNameNew)).getText().toString());
                        navigationFragmentSetup();
                        uv.kill();
                        break;
                }
            }
        };

        setAccountName((TextView) view.findViewById(R.id.accountEditNameCurrent));
        view.findViewById(R.id.accountEditNameApply).setOnClickListener(c);

    }

    private void setupAuthScreen(UniversalViewer uv, View view) {
        UniversalViewerListener c = new UniversalViewerListener(uv, view) {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.authEnter:
                        Notifications.makeToast( API.login(
                                ((EditText) view.findViewById(R.id.authLogin)).getText().toString(),
                                ((EditText) view.findViewById(R.id.authPassword)).getText().toString()
                        ) + "");
                        break;
                    case R.id.authEnterAsGuest:
                        if (!API.loginAsGuest(getResources().getString(R.string.menu_user_is_guest))) {
                            Notifications.makeToast( getResources().getString(R.string.auth_enter_as_guest_failed));
                            return;
                        }

                        navigationFragmentSetup();
                        uv.finish();
                        break;
                    case R.id.authRestore:
                        Notifications.makeToast( API.restoreAccount(
                                ((EditText) view.findViewById(R.id.authLogin)).getText().toString()
                        ) + "");
                        break;

                }
            }
        };

        view.findViewById(R.id.authEnter).setOnClickListener(c);
        view.findViewById(R.id.authEnterAsGuest).setOnClickListener(c);
        view.findViewById(R.id.authRestore).setOnClickListener(c);
    }

    private void setupDashboardScreen(UniversalViewer uv, View view) {

    }

    private void setupMessagesScreen(UniversalViewer uv, View view) {

    }

    private void setupNotificationsScreen(UniversalViewer uv, View view) {

    }

    private void setupStorageScreen(UniversalViewer uv, View view) {

    }

    private void setupModulesScreen(UniversalViewer uv, View view) {
        UniversalViewerListener c = new UniversalViewerListener(uv, view) {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.modulesAdd:
                        Intent i = new Intent(getContext(), UniversalViewer.class);
                        i.putExtra("layout", R.layout.menu_modules_add);
                        i.putExtra("needReload", true);
                        uv.startActivityForResult(i, 0);
                        break;
                }
            }
        };

        view.findViewById(R.id.modulesAdd).setOnClickListener(c);

        RecyclerView content = view.findViewById(R.id.modulesContent);
        content.setLayoutManager(new LinearLayoutManager(MainActivity.getAppContext()));

        HashMap<Integer, ModuleOption> modules = Modules.getModules();
        ArrayList<ModuleOption> list = new ArrayList<>();

        for (Map.Entry<Integer, ModuleOption> i: modules.entrySet()) {
            list.add(i.getValue());
        }

        ModulesAdapter.Listener listener = new ModulesAdapter.Listener() {
            @Override
            public void onClick(View v) {
                Notifications.makeToast(((TextView) v.findViewById(R.id.modulesItemSerial)).getText().toString());
            }
        };

        content.setAdapter(new ModulesAdapter(listener));
        ((ModulesAdapter) content.getAdapter()).setItems(list);

    }

    private void setupSyncScreen(UniversalViewer uv, View view) {
        class ExtendedListener extends UniversalViewerListener {

            private Switch sync = view.findViewById(R.id.syncSyncSwitcher);
            private CheckBox dashboard = view.findViewById(R.id.syncDashboardCheckbox);
            private CheckBox messages = view.findViewById(R.id.syncMessagesCheckbox);
            private CheckBox notifications = view.findViewById(R.id.syncNotificationsCheckbox);
            private Switch masterServer = view.findViewById(R.id.syncMasterServerSwitcher);
            private CheckBox markAsHomeNetwork = view.findViewById(R.id.syncMarkNetworkAsHomeCheckbox);
            private TextView currentNetwork = view.findViewById(R.id.syncCurrentNetwork);
            private TextView homeNetwork = view.findViewById(R.id.syncHomeNetwork);

            ExtendedListener(UniversalViewer uv, View view) {
                super(uv, view);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.syncSync:
                        sync.toggle();
                        Data.set("Sync", sync.isChecked());
                        updateSyncState();
                        break;
                    case R.id.syncDashboard:
                        if (dashboard.isEnabled()) {
                            dashboard.toggle();
                            Data.set("SyncDashboard", dashboard.isChecked());
                            Dashboard.setSync(dashboard.isChecked());
                        }
                        break;
                    case R.id.syncMessages:
                        if (messages.isEnabled() && Data.getBoolean("MasterServer", false)) {
                            messages.toggle();
                            Data.set("SyncMessages", messages.isChecked());
                            //Messages.setSync(messages.isChecked());
                        }
                        break;
                    case R.id.syncNotifications:
                        if (notifications.isEnabled() && Data.getBoolean("MasterServer", false)) {
                            notifications.toggle();
                            Data.set("SyncNotifications", notifications.isChecked());
                            //Notifications.setSync(notifications.isChecked());
                        }
                        break;
                    case R.id.syncMasterServer:
                        masterServer.toggle();
                        Data.set("MasterServer", masterServer.isChecked());
                        updateMasterServerState();
                        break;
                    case R.id.syncMasterServerSettings:
                        Intent i = new Intent(getContext(), UniversalViewer.class);
                        i.putExtra("layout", R.layout.menu_sync_masterserver);
                        uv.startActivity(i);
                        break;
                    case R.id.syncMarkNetworkAsHome:
                        if (!markAsHomeNetwork.isChecked()) {
                            if (Sync.getNetworkState() == 2) {
                                Data.set("SyncHomeNetwork", Sync.getNetworkBSSID());
                                updateSyncAddressesState();
                            }
                        }
                        else {
                            Data.set("SyncHomeNetwork", null);
                            updateSyncAddressesState();
                        }
                        break;
                }

                Data.recordOptions();
            }

            private void updateSyncState() {
                if (sync.isChecked()) {
                    dashboard.setEnabled(true);
                    messages.setEnabled(Data.getBoolean("MasterServer", false));
                    notifications.setEnabled(Data.getBoolean("MasterServer", false));
                    Dashboard.setSync(Data.getBoolean("SyncDashboard", false));
                    //Messages.setSync(Data.getBoolean("SyncMessages", false));
                    //Notifications.setSync(Data.getBoolean("SyncNotifications", false));
                }
                else {
                    Dashboard.setSync(false);
                    //Messages.setSync(false);
                    //Notifications.setSync(false);
                    dashboard.setEnabled(false);
                    messages.setEnabled(false);
                    notifications.setEnabled(false);
                }
            }

            private void updateMasterServerState() {
                view.findViewById(R.id.syncMasterServerSettings).setEnabled(
                        masterServer.isChecked()
                );

                updateSyncState();
            }

            private void updateSyncAddressesState() {
                markAsHomeNetwork.setChecked(
                        Data.getString("SyncHomeNetwork", "false").equals(Sync.getNetworkBSSID())
                );

                currentNetwork.setText(String.format("%s %s",
                        MainActivity.getAppResources().getString(R.string.masterServerCurrentNetwork),
                        Sync.getNetworkState() == 2 ? Sync.getNetworkBSSID() :
                                MainActivity.getAppResources().getString(R.string.sync_wifi_disabled)
                ));

                homeNetwork.setText(String.format("%s %s",
                        MainActivity.getAppResources().getString(R.string.masterServerHomeNetwork),
                        Data.getString("SyncHomeNetwork",
                                MainActivity.getAppResources().getString(R.string.sync_undefined))
                ));
            }
        }

        ExtendedListener c = new ExtendedListener(uv, view);

        c.sync.setChecked(
                Data.getBoolean("Sync", false)
        );

        c.dashboard.setChecked(
                Data.getBoolean("SyncDashboard", false)
        );

        c.messages.setChecked(
                Data.getBoolean("SyncMessages", false)
        );

        c.notifications.setChecked(
                Data.getBoolean("SyncNotifications", false)
        );

        c.masterServer.setChecked(
                Data.getBoolean("MasterServer", false)
        );

        c.updateMasterServerState();
        c.updateSyncState();
        c.updateSyncAddressesState();

        int[] buttons = {
                R.id.syncSync,
                R.id.syncDashboard,
                R.id.syncMessages,
                R.id.syncNotifications,
                R.id.syncMasterServer,
                R.id.syncMasterServerSettings,
                R.id.syncMarkNetworkAsHome
        };

        for (int i: buttons) {
            view.findViewById(i).setOnClickListener(c);
        }
    }

    private void setupSyncMasterServerScreen(UniversalViewer uv, View view) {
        InputFilter[] filters = new InputFilter[1];

        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);

                    if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    }

                    else {
                        String[] splits = resultingTxt.split("\\.");
                        for (String i : splits) {
                            if (Integer.valueOf(i) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }

        };

        ((EditText) view.findViewById(R.id.masterserverIP)).setFilters(filters);
    }

    private void setupAboutScreen(UniversalViewer uv, View view) {

    }
}
