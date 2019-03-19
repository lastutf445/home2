package lastutf445.android.com.home2;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
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
        id = "menu";
        return view;
    }

    @Override
    public void navigationFragmentSetup() {
        if (view == null) return;

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
                i.putExtra("needReload", true);
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

        try {
            if (i.hasExtra("layout")) {
                i.putExtra("returnCode", 0);
                getActivity().startActivityForResult(i, 0);
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int resultCode, Intent data) {
        Log.d("LOGTAG", "captured by menu - " + resultCode);
        if (data == null) return;

        if (data.getBooleanExtra("needReload", false)) {
            navigationFragmentSetup();
        }

    }

    @Override
    public void universalViewerSetup(UniversalViewer uv, View view, int layout) {
        //super.universalViewerSetup(view, layout);

        switch(layout) {
            case R.layout.menu_account:
                UniversalViewerScreens.setupAccountScreen(uv, view);
                break;
            case R.layout.menu_account_edit_name:
                UniversalViewerScreens.setupAccountEditName(uv, view);
                break;
            case R.layout.menu_auth:
                UniversalViewerScreens.setupAuthScreen(uv, view);
                break;
            case R.layout.menu_dashboard:
                UniversalViewerScreens.setupDashboardScreen(uv, view);
                break;
            case R.layout.menu_messages:
                UniversalViewerScreens.setupMessagesScreen(uv, view);
                break;
            case R.layout.menu_notifications:
                UniversalViewerScreens.setupNotificationsScreen(uv, view);
                break;
            case R.layout.menu_storage:
                UniversalViewerScreens.setupStorageScreen(uv, view);
                break;
            case R.layout.menu_modules:
                UniversalViewerScreens.setupModulesScreen(uv, view);
                break;
            case R.layout.menu_sync:
                UniversalViewerScreens.setupSyncScreen(uv, view);
                break;
            case R.layout.menu_sync_masterserver:
                UniversalViewerScreens.setupSyncMasterServerScreen(uv, view);
                break;
            case R.layout.menu_about:
                UniversalViewerScreens.setupAboutScreen(uv, view);
                break;
        }
    }

    public static void setAccountName(TextView view) {
        String accountName = Data.getString("AccountName", null);

        if (accountName == null) {
            accountName = MainActivity.getAppResources().getString(R.string.menu_account_name_error);
        }

        view.setText(accountName);
    }
}
