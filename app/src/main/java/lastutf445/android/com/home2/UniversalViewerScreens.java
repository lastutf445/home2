package lastutf445.android.com.home2;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UniversalViewerScreens {

    public static void setupAccountScreen(UniversalViewer uv, View view) {
        UniversalViewerListener c = new UniversalViewerListener(uv, view) {
            @Override
            public void onClick(View v) {
                switch(v.getId()) {
                    case R.id.accountEditAccountName:
                        Intent i = new Intent(uv, UniversalViewer.class);
                        i.putExtra("layout", R.layout.menu_account_edit_name);
                        i.putExtra("needReload", true);
                        uv.startActivityForResult(i, 0);
                        break;
                    case R.id.accountEditPrivacy:

                        break;
                    case R.id.accountLogout:
                        API.logout();
                        uv.kill();
                        break;
                }
            }
        };

        Menu.setAccountName((TextView) view.findViewById(R.id.accountName));

        view.findViewById(R.id.accountEditAccountName).setOnClickListener(c);
        view.findViewById(R.id.accountEditPrivacy).setOnClickListener(c);
        view.findViewById(R.id.accountLogout).setOnClickListener(c);
    }

    public static void setupAccountEditName(UniversalViewer uv, View view) {
        UniversalViewerListener c = new UniversalViewerListener(uv, view) {
            @Override
            public void onClick(View v) {
                switch(v.getId()) {
                    case R.id.accountEditNameApply:
                        API.rename(((TextView) view.findViewById(R.id.accountEditNameNew)).getText().toString());
                        uv.kill();
                        break;
                }
            }
        };

        Menu.setAccountName((TextView) view.findViewById(R.id.accountEditNameCurrent));
        view.findViewById(R.id.accountEditNameApply).setOnClickListener(c);
    }

    public static void setupAuthScreen(UniversalViewer uv, View view) {
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
                        if (!API.loginAsGuest(MainActivity.getAppContext().getString(R.string.menu_user_is_guest))) {
                            Notifications.makeToast(MainActivity.getAppContext().getString(R.string.auth_enter_as_guest_failed));
                            return;
                        }
                        uv.kill();
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

    public static void setupDashboardScreen(UniversalViewer uv, View view) {

    }

    public static void setupMessagesScreen(UniversalViewer uv, View view) {

    }

    public static void setupNotificationsScreen(UniversalViewer uv, View view) {

    }

    public static void setupStorageScreen(UniversalViewer uv, View view) {

    }

    public static void setupModulesScreen(UniversalViewer uv, View view) {
        UniversalViewerListener c = new UniversalViewerListener(uv, view) {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.modulesAdd:
                        Intent i = new Intent(uv, UniversalViewer.class);
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

    public static void setupSyncScreen(UniversalViewer uv, View view) {
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
                        Intent i = new Intent(uv, UniversalViewer.class);
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

    public static void setupSyncMasterServerScreen(UniversalViewer uv, View view) {
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

    public static void setupAboutScreen(UniversalViewer uv, View view) {

    }
}
