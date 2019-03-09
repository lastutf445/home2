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
            ExtendedListener(UniversalViewer uv, View view) {
                super(uv, view);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.syncSync:
                        Switch s = v.findViewById(R.id.syncSyncSwitcher);
                        s.toggle();
                        Data.set("Sync", s.isChecked());
                        updateSyncState(view);
                        break;
                    case R.id.syncDashboard:
                        CheckBox dashboard = v.findViewById(R.id.syncDashboardCheckbox);
                        if (dashboard.isEnabled() && Data.getBoolean("MasterServer", false)) {
                            dashboard.toggle();
                            Data.set("SyncDashboard", dashboard.isChecked());
                            //Dashboard.setSync(dashboard.isChecked());
                        }
                        break;
                    case R.id.syncMessages:
                        CheckBox messages = v.findViewById(R.id.syncMessagesCheckbox);
                        if (messages.isEnabled() && Data.getBoolean("MasterServer", false)) {
                            messages.toggle();
                            Data.set("SyncMessages", messages.isChecked());
                            //Messages.setSync(dashboard.isChecked());
                        }
                        break;
                    case R.id.syncNotifications:
                        CheckBox notifications = v.findViewById(R.id.syncNotificationsCheckbox);
                        if (notifications.isEnabled() && Data.getBoolean("MasterServer", false)) {
                            notifications.toggle();
                            Data.set("SyncNotifications", notifications.isChecked());
                            //Notifications.setSync(dashboard.isChecked());
                        }
                        break;
                    case R.id.syncMasterServer:
                        Switch masterServer = v.findViewById(R.id.syncMasterServerSwitcher);
                        masterServer.toggle();
                        Data.set("MasterServer", masterServer.isChecked());
                        updateMasterServerState(view);
                        break;
                    case R.id.syncMasterServerSettings:
                        Intent i = new Intent(getContext(), UniversalViewer.class);
                        i.putExtra("layout", R.layout.menu_sync_masterserver);
                        uv.startActivity(i);
                        break;
                }

                Data.recordOptions();
            }

            private void updateSyncState(View v) {
                Switch enabler = v.findViewById(R.id.syncSyncSwitcher);
                CheckBox dashboard = v.findViewById(R.id.syncDashboardCheckbox);
                CheckBox messages = v.findViewById(R.id.syncMessagesCheckbox);
                CheckBox notifications = v.findViewById(R.id.syncNotificationsCheckbox);

                if (enabler.isChecked()) {
                    dashboard.setEnabled(true);
                    messages.setEnabled(Data.getBoolean("MasterServer", false));
                    notifications.setEnabled(Data.getBoolean("MasterServer", false));
                }

                else {
                    dashboard.setEnabled(false);
                    messages.setEnabled(false);
                    notifications.setEnabled(false);
                }
            }

            private void updateMasterServerState(View v) {
                v.findViewById(R.id.syncMasterServerSettings).setEnabled(
                        ((Switch) v.findViewById(R.id.syncMasterServerSwitcher)).isChecked()
                );

                updateSyncState(v);
            }
        }

        ((Switch) view.findViewById(R.id.syncSyncSwitcher)).setChecked(
                Data.getBoolean("Sync", false)
        );

        ((CheckBox) view.findViewById(R.id.syncDashboardCheckbox)).setChecked(
                Data.getBoolean("SyncDashboard", false)
        );

        ((CheckBox) view.findViewById(R.id.syncMessagesCheckbox)).setChecked(
                Data.getBoolean("SyncMessages", false)
        );

        ((CheckBox) view.findViewById(R.id.syncNotificationsCheckbox)).setChecked(
                Data.getBoolean("SyncNotifications", false)
        );

        ((Switch) view.findViewById(R.id.syncMasterServerSwitcher)).setChecked(
                Data.getBoolean("MasterServer", false)
        );

        ExtendedListener c = new ExtendedListener(uv, view);

        c.updateMasterServerState(view);
        c.updateSyncState(view);

        int[] buttons = {
                R.id.syncSync,
                R.id.syncDashboard,
                R.id.syncMessages,
                R.id.syncNotifications,
                R.id.syncMasterServer,
                R.id.syncMasterServerSettings

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

        ((EditText)view.findViewById(R.id.masterserverIP)).setFilters(filters);
    }

    private void setupAboutScreen(UniversalViewer uv, View view) {

    }
}
